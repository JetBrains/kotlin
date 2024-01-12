/*
 * Copyright 2010-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.overrides

import org.jetbrains.kotlin.backend.common.linkage.partial.ImplementAsErrorThrowingStubs
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureFactory
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildTypeParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.overrides.IrUnimplementedOverridesStrategy
import org.jetbrains.kotlin.ir.overrides.IrUnimplementedOverridesStrategy.ProcessAsFakeOverrides
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

class FakeOverrideGlobalDeclarationTable(
    mangler: KotlinMangler.IrMangler
) : GlobalDeclarationTable(mangler) {
    fun clear() = table.clear()
}

open class FakeOverrideDeclarationTable(
    mangler: KotlinMangler.IrMangler,
    globalTable: FakeOverrideGlobalDeclarationTable = FakeOverrideGlobalDeclarationTable(mangler),
    signatureSerializerFactory: (PublicIdSignatureComputer, DeclarationTable) -> IdSignatureFactory
) : DeclarationTable(globalTable) {
    override val globalDeclarationTable: FakeOverrideGlobalDeclarationTable = globalTable
    override val signaturer: IdSignatureFactory = signatureSerializerFactory(globalTable.publicIdSignatureComputer, this)

    fun clear() {
        this.table.clear()
        globalDeclarationTable.clear()
    }
}

interface FakeOverrideClassFilter {
    fun needToConstructFakeOverrides(clazz: IrClass): Boolean
}

interface FileLocalAwareLinker {
    fun tryReferencingSimpleFunctionByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrSimpleFunctionSymbol?
    fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol?
}

object DefaultFakeOverrideClassFilter : FakeOverrideClassFilter {
    override fun needToConstructFakeOverrides(clazz: IrClass): Boolean = true
}

private class IrLinkerFakeOverrideBuilderStrategy(
    val linker: FileLocalAwareLinker,
    val symbolTable: SymbolTable,
    private val irBuiltIns: IrBuiltIns,
    private val partialLinkageSupport: PartialLinkageSupportForLinker,
    private val fakeOverrideDeclarationTable: DeclarationTable,
    friendModules: Map<String, Collection<String>>,
    unimplementedOverridesStrategy: IrUnimplementedOverridesStrategy,
) : FakeOverrideBuilderStrategy(
    friendModules = friendModules,
    unimplementedOverridesStrategy = unimplementedOverridesStrategy
) {

    override fun <R> inFile(file: IrFile?, block: () -> R): R =
        fakeOverrideDeclarationTable.inFile(file, block)

    override fun linkFunctionFakeOverride(function: IrFunctionWithLateBinding, manglerCompatibleMode: Boolean) {
        val (signature, symbol) = computeFunctionFakeOverrideSymbol(function, manglerCompatibleMode)

        symbolTable.declareSimpleFunction(signature, { symbol }) {
            assert(it === symbol)
            function.acquireSymbol(it)
        }
    }

    override fun linkPropertyFakeOverride(property: IrPropertyWithLateBinding, manglerCompatibleMode: Boolean) {
        // To compute a signature for a property with type parameters,
        // we must have its accessor's correspondingProperty pointing to the property's symbol.
        // See IrMangleComputer.mangleTypeParameterReference() for details.
        // But to create and link that symbol we should already have the signature computed.
        // To break this loop we use temp symbol in correspondingProperty.

        val tempSymbol = IrPropertySymbolImpl().also {
            it.bind(property as IrProperty)
        }
        property.getter?.let { getter ->
            getter.correspondingPropertySymbol = tempSymbol
        }
        property.setter?.let { setter ->
            setter.correspondingPropertySymbol = tempSymbol
        }

        val (signature, symbol) = computePropertyFakeOverrideSymbol(property, manglerCompatibleMode)
        symbolTable.declareProperty(signature, { symbol }) {
            assert(it === symbol)
            property.acquireSymbol(it)
        }

        property.getter?.let { getter ->
            getter.correspondingPropertySymbol = property.symbol
            linkFunctionFakeOverride(
                getter as? IrFunctionWithLateBinding ?: error("Unexpected fake override getter: $getter"),
                manglerCompatibleMode
            )
        }
        property.setter?.let { setter ->
            setter.correspondingPropertySymbol = property.symbol
            linkFunctionFakeOverride(
                setter as? IrFunctionWithLateBinding ?: error("Unexpected fake override setter: $setter"),
                manglerCompatibleMode
            )
        }
    }

    private fun composeSignature(declaration: IrDeclaration, manglerCompatibleMode: Boolean) =
        fakeOverrideDeclarationTable.signaturer.composeSignatureForDeclaration(declaration, manglerCompatibleMode)

    private fun computeFunctionFakeOverrideSymbol(
        function: IrFunctionWithLateBinding,
        manglerCompatibleMode: Boolean
    ): Pair<IdSignature, IrSimpleFunctionSymbol> {
        val parent = function.parentAsClass

        val signature = composeSignature(function, manglerCompatibleMode)
        val symbol = linker.tryReferencingSimpleFunctionByLocalSignature(parent, signature)
            ?: symbolTable.referenceSimpleFunction(signature)

        if (!partialLinkageSupport.isEnabled
            || !symbol.isBound
            || symbol.owner.let { boundFunction ->
                boundFunction.isSuspend == function.isSuspend && !boundFunction.isInline && !function.isInline
            }
        ) {
            return signature to symbol
        }

        // In old KLIB signatures we don't distinguish between suspend and non-suspend, inline and non-inline functions. So we need to
        // manually patch the signature of the fake override to avoid clash with the existing function with the different `isSuspend` flag
        // state or the existing function with `isInline=true`.
        // This signature is not supposed to be ever serialized (as fake overrides are not serialized in KLIBs).
        // In new KLIB signatures `isSuspend` and `isInline` flags will be taken into account as a part of signature.
        val functionWithDisambiguatedSignature = buildFunctionWithDisambiguatedSignature(function)
        val disambiguatedSignature = composeSignature(functionWithDisambiguatedSignature, manglerCompatibleMode)
        assert(disambiguatedSignature != signature) { "Failed to compute disambiguated signature for fake override $function" }

        val symbolWithDisambiguatedSignature = linker.tryReferencingSimpleFunctionByLocalSignature(parent, disambiguatedSignature)
            ?: symbolTable.referenceSimpleFunction(disambiguatedSignature)

        return disambiguatedSignature to symbolWithDisambiguatedSignature
    }

    private fun computePropertyFakeOverrideSymbol(
        property: IrPropertyWithLateBinding,
        manglerCompatibleMode: Boolean
    ): Pair<IdSignature, IrPropertySymbol> {
        val parent = property.parentAsClass

        val signature = composeSignature(property, manglerCompatibleMode)
        val symbol = linker.tryReferencingPropertyByLocalSignature(parent, signature)
            ?: symbolTable.referenceProperty(signature)

        if (!partialLinkageSupport.isEnabled
            || !symbol.isBound
            || symbol.owner.let { boundProperty ->
                boundProperty.getter?.isInline != true && boundProperty.setter?.isInline != true
                        && property.getter?.isInline != true && property.setter?.isInline != true
            }
        ) {
            return signature to symbol
        }

        // In old KLIB signatures we don't distinguish between inline and non-inline property accessors. So we need to
        // manually patch the signature of the fake override to avoid clash with the existing property with `inline` accessors.
        // This signature is not supposed to be ever serialized (as fake overrides are not serialized in KLIBs).
        // In new KLIB signatures `isInline` flag will be taken into account as a part of signature.

        val propertyWithDisambiguatedSignature = buildPropertyWithDisambiguatedSignature(property)
        val disambiguatedSignature = composeSignature(propertyWithDisambiguatedSignature, manglerCompatibleMode)
        assert(disambiguatedSignature != signature) { "Failed to compute disambiguated signature for fake override $property" }

        val symbolWithDisambiguatedSignature = linker.tryReferencingPropertyByLocalSignature(parent, disambiguatedSignature)
            ?: symbolTable.referenceProperty(disambiguatedSignature)

        return disambiguatedSignature to symbolWithDisambiguatedSignature
    }

    private fun buildFunctionWithDisambiguatedSignature(function: IrSimpleFunction): IrSimpleFunction =
        function.factory.buildFun {
            updateFrom(function)
            name = function.name
            returnType = irBuiltIns.unitType // Does not matter.
        }.apply {
            parent = function.parent
            copyAnnotationsFrom(function)
            copyParameterDeclarationsFrom(function)

            typeParameters = typeParameters + buildTypeParameter(this) {
                name = Name.identifier("disambiguation type parameter")
                index = typeParameters.size
                superTypes += irBuiltIns.nothingType // This is something that can't be expressed in the source code.
            }
        }

    private fun buildPropertyWithDisambiguatedSignature(property: IrProperty): IrProperty =
        property.factory.buildProperty {
            updateFrom(property)
            name = property.name
        }.apply {
            parent = property.parent
            copyAnnotationsFrom(property)

            getter = property.getter?.let { buildFunctionWithDisambiguatedSignature(it) }
            setter = property.setter?.let { buildFunctionWithDisambiguatedSignature(it) }
        }
}

class IrLinkerFakeOverrideProvider(
    linker: FileLocalAwareLinker,
    symbolTable: SymbolTable,
    mangler: KotlinMangler.IrMangler,
    typeSystem: IrTypeSystemContext,
    friendModules: Map<String, Collection<String>>,
    private val partialLinkageSupport: PartialLinkageSupportForLinker,
    val platformSpecificClassFilter: FakeOverrideClassFilter = DefaultFakeOverrideClassFilter,
    private val fakeOverrideDeclarationTable: DeclarationTable = FakeOverrideDeclarationTable(mangler) { builder, table ->
        IdSignatureFactory(builder, table)
    },
    externalOverridabilityConditions: List<IrExternalOverridabilityCondition> = emptyList(),
) {
    private val irFakeOverrideBuilder = IrFakeOverrideBuilder(
        typeSystem,
        IrLinkerFakeOverrideBuilderStrategy(
            linker,
            symbolTable,
            typeSystem.irBuiltIns,
            partialLinkageSupport,
            fakeOverrideDeclarationTable,
            friendModules,
            if (partialLinkageSupport.isEnabled)
                ImplementAsErrorThrowingStubs(partialLinkageSupport)
            else
                ProcessAsFakeOverrides
        ),
        externalOverridabilityConditions
    )

    private val haveFakeOverrides = mutableSetOf<IrClass>()
    val fakeOverrideCandidates = mutableMapOf<IrClass, CompatibilityMode>()

    fun enqueueClass(clazz: IrClass, signature: IdSignature, compatibilityMode: CompatibilityMode) {
        fakeOverrideDeclarationTable.assumeDeclarationSignature(clazz, signature)
        fakeOverrideCandidates[clazz] = compatibilityMode
    }

    private fun buildFakeOverrideChainsForClass(clazz: IrClass, compatibilityMode: CompatibilityMode): Boolean {
        if (haveFakeOverrides.contains(clazz)) return true

        for (supertype in clazz.superTypes) {
            val superClass = supertype.getClass() ?: error("Unexpected super type: ${supertype.render()}")
            val mode = fakeOverrideCandidates[superClass] ?: compatibilityMode
            if (buildFakeOverrideChainsForClass(superClass, mode))
                haveFakeOverrides.add(superClass)
        }

        if (!platformSpecificClassFilter.needToConstructFakeOverrides(clazz)) return false

        irFakeOverrideBuilder.buildFakeOverridesForClass(clazz, compatibilityMode.oldSignatures)

        return true
    }

    fun provideFakeOverrides(klass: IrClass, compatibilityMode: CompatibilityMode) {
        buildFakeOverrideChainsForClass(klass, compatibilityMode)
        haveFakeOverrides.add(klass)
    }

    fun provideFakeOverrides() {
        val entries = fakeOverrideCandidates.entries.toMutableList()
        while (entries.isNotEmpty()) {
            val candidate = entries.removeLast()
            provideFakeOverrides(candidate.key, candidate.value)
        }
        fakeOverrideCandidates.clear()
    }
}
