/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

@OptIn(ObsoleteDescriptorBasedAPI::class)
open class DeepCopySymbolRemapper(
    private val descriptorsRemapper: DescriptorsRemapper = NullDescriptorsRemapper
) : IrElementVisitorVoid, SymbolRemapper {

    protected var classes = hashMapOf<IrClassSymbol, IrClassSymbol>()
    protected var scripts = hashMapOf<IrScriptSymbol, IrScriptSymbol>()
    protected var constructors = hashMapOf<IrConstructorSymbol, IrConstructorSymbol>()
    protected var enumEntries = hashMapOf<IrEnumEntrySymbol, IrEnumEntrySymbol>()
    protected var externalPackageFragments = hashMapOf<IrExternalPackageFragmentSymbol, IrExternalPackageFragmentSymbol>()
    protected var fields = hashMapOf<IrFieldSymbol, IrFieldSymbol>()
    protected var files = hashMapOf<IrFileSymbol, IrFileSymbol>()
    protected var functions = hashMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()
    protected var properties = hashMapOf<IrPropertySymbol, IrPropertySymbol>()
    protected var returnableBlocks = hashMapOf<IrReturnableBlockSymbol, IrReturnableBlockSymbol>()
    protected var typeParameters = hashMapOf<IrTypeParameterSymbol, IrTypeParameterSymbol>()
    protected var valueParameters = hashMapOf<IrValueParameterSymbol, IrValueParameterSymbol>()
    protected var variables = hashMapOf<IrVariableSymbol, IrVariableSymbol>()
    protected var localDelegatedProperties = hashMapOf<IrLocalDelegatedPropertySymbol, IrLocalDelegatedPropertySymbol>()
    protected var typeAliases = hashMapOf<IrTypeAliasSymbol, IrTypeAliasSymbol>()

    fun addMappingsFrom(other: DeepCopySymbolRemapper) {
        classes += other.classes
        scripts += other.scripts
        constructors += other.constructors
        enumEntries += other.enumEntries
        externalPackageFragments += other.externalPackageFragments
        fields += other.fields
        files += other.files
        functions += other.functions
        properties += other.properties
        returnableBlocks += other.returnableBlocks
        typeParameters += other.typeParameters
        valueParameters += other.valueParameters
        variables += other.variables
        localDelegatedProperties += other.localDelegatedProperties
        typeAliases += other.typeAliases
    }

    fun replaceKeys(remapper: DeepCopySymbolRemapper) {
        classes = classes.mapKeysTo(hashMapOf()) { remapper.getReferencedClass(it.key) }
        scripts = scripts.mapKeysTo(hashMapOf()) { remapper.getReferencedScript(it.key) }
        constructors = constructors.mapKeysTo(hashMapOf()) { remapper.getReferencedConstructor(it.key) }
        enumEntries = enumEntries.mapKeysTo(hashMapOf()) { remapper.getReferencedEnumEntry(it.key) }
        fields = fields.mapKeysTo(hashMapOf()) { remapper.getReferencedField(it.key) }
        functions = functions.mapKeysTo(hashMapOf()) { remapper.getReferencedSimpleFunction(it.key) }
        properties = properties.mapKeysTo(hashMapOf()) { remapper.getReferencedProperty(it.key) }
        returnableBlocks = returnableBlocks.mapKeysTo(hashMapOf()) { remapper.getReferencedReturnableBlock(it.key) }
        typeParameters = typeParameters.mapKeysTo(hashMapOf()) { remapper.getReferencedTypeParameter(it.key) }
        valueParameters = valueParameters.mapKeysTo(hashMapOf()) { remapper.getReferencedValueParameter(it.key) }
        variables = variables.mapKeysTo(hashMapOf()) { remapper.getReferencedVariable(it.key) }
        localDelegatedProperties = localDelegatedProperties.mapKeysTo(hashMapOf()) { remapper.getReferencedLocalDelegatedProperty(it.key) }
        typeAliases = typeAliases.mapKeysTo(hashMapOf()) { remapper.getReferencedTypeAlias(it.key) }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    protected inline fun <D : DeclarationDescriptor, B : IrSymbolOwner, reified S : IrBindableSymbol<D, B>>
            remapSymbol(map: MutableMap<S, S>, owner: B, createNewSymbol: (S) -> S) {
        val symbol = owner.symbol as S
        map[symbol] = createNewSymbol(symbol)
    }

    override fun visitClass(declaration: IrClass) {
        remapSymbol(classes, declaration) {
            IrClassSymbolImpl(descriptorsRemapper.remapDeclaredClass(it.descriptor))
        }
        super.visitClass(declaration)
    }

    override fun visitScript(declaration: IrScript) {
        remapSymbol(scripts, declaration) {
            IrScriptSymbolImpl(descriptorsRemapper.remapDeclaredScript(it.descriptor))
        }
        super.visitScript(declaration)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        remapSymbol(constructors, declaration) {
            IrConstructorSymbolImpl(descriptorsRemapper.remapDeclaredConstructor(it.descriptor))
        }
        super.visitConstructor(declaration)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        remapSymbol(enumEntries, declaration) {
            IrEnumEntrySymbolImpl(descriptorsRemapper.remapDeclaredEnumEntry(it.descriptor))
        }
        super.visitEnumEntry(declaration)
    }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) {
        remapSymbol(externalPackageFragments, declaration) {
            IrExternalPackageFragmentSymbolImpl(descriptorsRemapper.remapDeclaredExternalPackageFragment(it.descriptor))
        }
        super.visitExternalPackageFragment(declaration)
    }

    override fun visitField(declaration: IrField) {
        remapSymbol(fields, declaration) {
            IrFieldSymbolImpl(descriptorsRemapper.remapDeclaredField(it.descriptor))
        }
        super.visitField(declaration)
    }

    override fun visitFile(declaration: IrFile) {
        remapSymbol(files, declaration) {
            IrFileSymbolImpl(descriptorsRemapper.remapDeclaredFilePackageFragment(it.descriptor))
        }
        super.visitFile(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        remapSymbol(functions, declaration) {
            IrSimpleFunctionSymbolImpl(descriptorsRemapper.remapDeclaredSimpleFunction(it.descriptor))
        }
        super.visitSimpleFunction(declaration)
    }

    override fun visitProperty(declaration: IrProperty) {
        remapSymbol(properties, declaration) {
            IrPropertySymbolImpl(descriptorsRemapper.remapDeclaredProperty(it.descriptor))
        }
        super.visitProperty(declaration)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        remapSymbol(typeParameters, declaration) {
            IrTypeParameterSymbolImpl(descriptorsRemapper.remapDeclaredTypeParameter(it.descriptor))
        }
        super.visitTypeParameter(declaration)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        remapSymbol(valueParameters, declaration) {
            IrValueParameterSymbolImpl(descriptorsRemapper.remapDeclaredValueParameter(it.descriptor))
        }
        super.visitValueParameter(declaration)
    }

    override fun visitVariable(declaration: IrVariable) {
        remapSymbol(variables, declaration) {
            IrVariableSymbolImpl(descriptorsRemapper.remapDeclaredVariable(it.descriptor))
        }
        super.visitVariable(declaration)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        remapSymbol(localDelegatedProperties, declaration) {
            IrLocalDelegatedPropertySymbolImpl(descriptorsRemapper.remapDeclaredLocalDelegatedProperty(it.descriptor))
        }
        super.visitLocalDelegatedProperty(declaration)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        remapSymbol(typeAliases, declaration) {
            IrTypeAliasSymbolImpl(descriptorsRemapper.remapDeclaredTypeAlias(it.descriptor))
        }
        super.visitTypeAlias(declaration)
    }

    override fun visitReturnableBlock(expression: IrReturnableBlock) {
        remapSymbol(returnableBlocks, expression) {
            IrReturnableBlockSymbolImpl()
        }
        super.visitReturnableBlock(expression)
    }

    private fun <T : IrSymbol> Map<T, T>.getDeclared(symbol: T) =
        getOrElse(symbol) {
            throw IllegalArgumentException("Non-remapped symbol $symbol")
        }

    private fun <T : IrSymbol> Map<T, T>.getReferenced(symbol: T) =
        getOrElse(symbol) { symbol }

    override fun getDeclaredClass(symbol: IrClassSymbol): IrClassSymbol = classes.getDeclared(symbol)

    override fun getDeclaredAnonymousInitializer(symbol: IrAnonymousInitializerSymbol): IrAnonymousInitializerSymbol =
        IrAnonymousInitializerSymbolImpl(symbol.owner.descriptor)

    override fun getDeclaredScript(symbol: IrScriptSymbol): IrScriptSymbol = scripts.getDeclared(symbol)
    override fun getDeclaredSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = functions.getDeclared(symbol)
    override fun getDeclaredProperty(symbol: IrPropertySymbol): IrPropertySymbol = properties.getDeclared(symbol)
    override fun getDeclaredField(symbol: IrFieldSymbol): IrFieldSymbol = fields.getDeclared(symbol)
    override fun getDeclaredFile(symbol: IrFileSymbol): IrFileSymbol = files.getDeclared(symbol)
    override fun getDeclaredConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = constructors.getDeclared(symbol)
    override fun getDeclaredEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = enumEntries.getDeclared(symbol)
    override fun getDeclaredExternalPackageFragment(symbol: IrExternalPackageFragmentSymbol): IrExternalPackageFragmentSymbol =
        externalPackageFragments.getDeclared(symbol)

    override fun getDeclaredVariable(symbol: IrVariableSymbol): IrVariableSymbol = variables.getDeclared(symbol)
    override fun getDeclaredTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameterSymbol = typeParameters.getDeclared(symbol)
    override fun getDeclaredValueParameter(symbol: IrValueParameterSymbol): IrValueParameterSymbol = valueParameters.getDeclared(symbol)
    override fun getDeclaredLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol): IrLocalDelegatedPropertySymbol =
        localDelegatedProperties.getDeclared(symbol)

    override fun getDeclaredTypeAlias(symbol: IrTypeAliasSymbol): IrTypeAliasSymbol = typeAliases.getDeclared(symbol)

    override fun getDeclaredReturnableBlock(symbol: IrReturnableBlockSymbol) = returnableBlocks.getDeclared(symbol)

    override fun getReferencedClass(symbol: IrClassSymbol): IrClassSymbol = classes.getReferenced(symbol)
    override fun getReferencedScript(symbol: IrScriptSymbol): IrScriptSymbol = scripts.getReferenced(symbol)
    override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = enumEntries.getReferenced(symbol)
    override fun getReferencedVariable(symbol: IrVariableSymbol): IrVariableSymbol = variables.getReferenced(symbol)
    override fun getReferencedValueParameter(symbol: IrValueParameterSymbol): IrValueParameterSymbol = valueParameters.getReferenced(symbol)
    override fun getReferencedLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol): IrLocalDelegatedPropertySymbol =
        localDelegatedProperties.getReferenced(symbol)

    override fun getReferencedField(symbol: IrFieldSymbol): IrFieldSymbol = fields.getReferenced(symbol)
    override fun getReferencedConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = constructors.getReferenced(symbol)
    override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = functions.getReferenced(symbol)
    override fun getReferencedProperty(symbol: IrPropertySymbol): IrPropertySymbol = properties.getReferenced(symbol)

    override fun getReferencedReturnableBlock(symbol: IrReturnableBlockSymbol): IrReturnableBlockSymbol =
        returnableBlocks.getReferenced(symbol)

    override fun getReferencedTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameterSymbol = typeParameters.getReferenced(symbol)

    override fun getReferencedTypeAlias(symbol: IrTypeAliasSymbol): IrTypeAliasSymbol = typeAliases.getReferenced(symbol)
}