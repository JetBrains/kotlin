/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(InternalSymbolFinderAPI::class, UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.cli.jklib.pipeline

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Links the references of a deserialized inline function body against the declarations of the compilation that is
 * running.
 *
 * [org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer] reads the inline function
 * bodies of the dependencies into a symbol table of its own and deliberately leaves the symbols they refer to unbound:
 * the other KLIB backends serialize those bodies straight back into a KLIB, and it is the consumer of that KLIB that
 * links them.
 *
 * JKlib hands the IR over to its consumer directly, so it needs those references resolved here. It can do so because
 * the full transitive closure of the dependencies is always passed to the compiler, which means every referenced
 * declaration is reachable through `SymbolFinder`.
 *
 * Resolution is exact: a candidate is accepted only when the signature computed from it equals the requested one. When
 * nothing matches, `null` is returned and the reference is left unbound, which is the default behavior.
 */
internal class JKlibInlineFunctionSymbolResolver(context: PreSerializationLoweringContext) {

    private val irBuiltIns = context.irBuiltIns
    private val symbolFinder = irBuiltIns.symbolFinder
    private val signatureComputer = PublicIdSignatureComputer(context.irMangler)
    private val cache = HashMap<IdSignature, IrSymbol?>()

    /**
     * The IR intrinsics, such as `EQEQ`. They are synthesized by `IrBuiltIns` and have no FIR counterpart, so
     * `SymbolFinder` cannot see them, but the inline function bodies of the dependencies do refer to them.
     */
    private val builtInFunctions: List<IrSimpleFunction> by lazy {
        irBuiltIns.operatorsPackageFragment.declarations.filterIsInstance<IrSimpleFunction>()
    }

    fun resolve(signature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        if (cache.containsKey(signature)) return cache[signature]
        return resolveUncached(signature, symbolKind).also { cache[signature] = it }
    }

    private fun resolveUncached(signature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? =
        when (symbolKind) {
            BinarySymbolData.SymbolKind.CLASS_SYMBOL -> findClass(signature)
            BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> findConstructor(signature)
            BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> findEnumEntry(signature)
            BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> findFunction(signature)
            BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> findProperty(signature)
            BinarySymbolData.SymbolKind.FIELD_SYMBOL,
            BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> findField(signature)
            else -> null
        }

    /** A class is uniquely identified by its [ClassId], so no signature check is needed. */
    private fun findClass(signature: IdSignature): IrClassSymbol? {
        val common = signature.asPublic() ?: return null
        return symbolFinder.findClass(ClassId(FqName(common.packageFqName), FqName(common.declarationFqName), isLocal = false))
    }

    private fun findFunction(signature: IdSignature): IrSymbol? {
        if (signature is IdSignature.AccessorSignature) {
            val property = findProperty(signature.propertySignature)?.owner ?: return null
            val isGetter = signature.accessorSignature.shortName.startsWith(GETTER_PREFIX)
            return (if (isGetter) property.getter else property.setter)?.symbol
        }
        val common = signature.asPublic() ?: return null
        val candidates = containingClassOf(common)?.members<IrSimpleFunction>()
            ?: (symbolFinder.findFunctions(common.callableId()).map { it.owner } + builtInFunctions)
        return candidates.matching(signature) { it.name.asString() == common.shortName }?.symbol
    }

    private fun findProperty(signature: IdSignature): IrPropertySymbol? {
        val common = signature.asPublic() ?: return null
        val candidates = containingClassOf(common)?.members<IrProperty>()
            ?: symbolFinder.findProperties(common.callableId()).map { it.owner }
        return candidates.matching(signature) { it.name.asString() == common.shortName }?.symbol
    }

    private fun findConstructor(signature: IdSignature): IrSymbol? {
        val common = signature.asPublic() ?: return null
        return containingClassOf(common)?.members<IrConstructor>()?.matching(signature)?.symbol
    }

    private fun findEnumEntry(signature: IdSignature): IrSymbol? {
        val common = signature.asPublic() ?: return null
        return containingClassOf(common)
            ?.members<IrEnumEntry>()
            ?.matching(signature) { it.name.asString() == common.shortName }
            ?.symbol
    }

    /**
     * A backing field is signed as the signature of its property with the `<BF>` marker appended, see
     * `IdSignatureBuilder`. A field with no property of its own keeps a signature of its own.
     */
    private fun findField(signature: IdSignature): IrSymbol? {
        val propertySignature = signature.backingFieldOwnerOrNull()
        if (propertySignature != null) {
            return findProperty(propertySignature)?.owner?.backingField?.symbol
        }
        val common = signature.asPublic() ?: return null
        return containingClassOf(common)
            ?.members<IrField>()
            ?.matching(signature) { it.name.asString() == common.shortName }
            ?.symbol
    }

    private fun IdSignature.backingFieldOwnerOrNull(): IdSignature? =
        (this as? IdSignature.CompositeSignature)
            ?.takeIf { (it.inner as? IdSignature.LocalSignature)?.localFqn == MangleConstant.BACKING_FIELD_NAME }
            ?.container

    /**
     * The class a member declaration belongs to, or `null` when the declaration is top level. The last name segment of a
     * member signature is always the name of the member itself, so everything before it is the class it is nested in.
     */
    private fun containingClassOf(common: IdSignature.CommonSignature): IrClassSymbol? {
        val classSegments = common.nameSegments.dropLast(1)
        if (classSegments.isEmpty()) return null
        return symbolFinder.findClass(ClassId(FqName(common.packageFqName), FqName.fromSegments(classSegments), isLocal = false))
    }

    private inline fun <reified T : IrDeclaration> IrClassSymbol.members(): List<T> =
        owner.declarations.filterIsInstance<T>()

    private fun <T : IrDeclaration> List<T>.matching(
        signature: IdSignature,
        named: (T) -> Boolean = { true },
    ): T? = filter(named).firstOrNull { signatureComputer.computeSignature(it) == signature }

    private fun IdSignature.CommonSignature.callableId() =
        CallableId(FqName(packageFqName), Name.guessByFirstCharacter(shortName))

    private companion object {
        const val GETTER_PREFIX = "<get-"
    }
}
