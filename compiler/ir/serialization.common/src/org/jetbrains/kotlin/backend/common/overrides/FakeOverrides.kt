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

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.GlobalDeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.overrides.IrOverridingUtil
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.parentAsClass

class FakeOverrideGlobalDeclarationTable(
    mangler: KotlinMangler.IrMangler
) : GlobalDeclarationTable(mangler) {
    fun clear() = table.clear()
}

class FakeOverrideDeclarationTable(
    mangler: KotlinMangler.IrMangler,
    signatureSerializerFactory: (PublicIdSignatureComputer, DeclarationTable) -> IdSignatureSerializer,
    globalTable: FakeOverrideGlobalDeclarationTable = FakeOverrideGlobalDeclarationTable(mangler)
) : DeclarationTable(globalTable) {
    override val globalDeclarationTable: FakeOverrideGlobalDeclarationTable = globalTable
    override val signaturer: IdSignatureSerializer = signatureSerializerFactory(globalTable.publicIdSignatureComputer, this)
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

class FakeOverrideBuilder(
    val linker: FileLocalAwareLinker,
    val symbolTable: SymbolTable,
    mangler: KotlinMangler.IrMangler,
    irBuiltIns: IrBuiltIns,
    val platformSpecificClassFilter: FakeOverrideClassFilter = DefaultFakeOverrideClassFilter,
    val signatureSerializerFactory: (PublicIdSignatureComputer, DeclarationTable) -> IdSignatureSerializer = ::IdSignatureSerializer,
) : FakeOverrideBuilderStrategy() {
    private val haveFakeOverrides = mutableSetOf<IrClass>()

    private val irOverridingUtil = IrOverridingUtil(irBuiltIns, this)

    // TODO: The declaration table is needed for the signaturer.
    private val fakeOverrideDeclarationTable = FakeOverrideDeclarationTable(mangler, signatureSerializerFactory)

    private val fakeOverrideClassQueue = mutableListOf<IrClass>()
    fun enqueueClass(clazz: IrClass, signature: IdSignature) {
        fakeOverrideDeclarationTable.assumeDeclarationSignature(clazz, signature)
        fakeOverrideClassQueue.add(clazz)
    }

    private fun buildFakeOverrideChainsForClass(clazz: IrClass) {
        if (haveFakeOverrides.contains(clazz)) return
        if (!platformSpecificClassFilter.needToConstructFakeOverrides(clazz)) return

        val superTypes = clazz.superTypes

        val superClasses = superTypes.map {
            it.getClass() ?: error("Unexpected super type: $it")
        }

        superClasses.forEach {
            buildFakeOverrideChainsForClass(it)
            haveFakeOverrides.add(it)
        }

        irOverridingUtil.buildFakeOverridesForClass(clazz)
    }

    override fun linkFunctionFakeOverride(declaration: IrFakeOverrideFunction) {
        val signature = composeSignature(declaration)
        declareFunctionFakeOverride(declaration, signature)
    }

    override fun linkPropertyFakeOverride(declaration: IrFakeOverrideProperty) {
        // To compute a signature for a property with type parameters,
        // we must have its accessor's correspondingProperty pointing to the property's symbol.
        // See IrMangleComputer.mangleTypeParameterReference() for details.
        // But to create and link that symbol we should already have the signature computed.
        // To break this loop we use temp symbol in correspondingProperty.

        val tempSymbol = IrPropertySymbolImpl().also {
            it.bind(declaration as IrProperty)
        }
        declaration.getter?.let {
            it.correspondingPropertySymbol = tempSymbol
        }
        declaration.setter?.let {
            it.correspondingPropertySymbol = tempSymbol
        }

        val signature = composeSignature(declaration)
        declarePropertyFakeOverride(declaration, signature)

        declaration.getter?.let {
            it.correspondingPropertySymbol = declaration.symbol
            linkFunctionFakeOverride(it as? IrFakeOverrideFunction ?: error("Unexpected fake override getter: $it"))
        }
        declaration.setter?.let {
            it.correspondingPropertySymbol = declaration.symbol
            linkFunctionFakeOverride(it as? IrFakeOverrideFunction ?: error("Unexpected fake override setter: $it"))
        }
    }

    private fun composeSignature(declaration: IrDeclaration) =
        fakeOverrideDeclarationTable.signaturer.composeSignatureForDeclaration(declaration)

    private fun declareFunctionFakeOverride(declaration: IrFakeOverrideFunction, signature: IdSignature) {
        val parent = declaration.parentAsClass
        val symbol = linker.tryReferencingSimpleFunctionByLocalSignature(parent, signature)
            ?: symbolTable.referenceSimpleFunctionFromLinker(signature)
        symbolTable.declareSimpleFunction(signature, { symbol }) {
            assert(it === symbol)
            declaration.acquireSymbol(it)
        }
    }

    private fun declarePropertyFakeOverride(declaration: IrFakeOverrideProperty, signature: IdSignature) {
        val parent = declaration.parentAsClass
        val symbol = linker.tryReferencingPropertyByLocalSignature(parent, signature)
            ?: symbolTable.referencePropertyFromLinker(signature)
        symbolTable.declareProperty(signature, { symbol }) {
            assert(it === symbol)
            declaration.acquireSymbol(it)
        }
    }

    private fun provideFakeOverrides(klass: IrClass) {
        buildFakeOverrideChainsForClass(klass)
        irOverridingUtil.clear()
        haveFakeOverrides.add(klass)
    }

    fun provideFakeOverrides() {
        while (fakeOverrideClassQueue.isNotEmpty()) {
            val klass = fakeOverrideClassQueue.removeLast()
            provideFakeOverrides(klass)
        }
    }
}
