/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.irInterceptedLocal_hashMapOf
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

@OptIn(ObsoleteDescriptorBasedAPI::class)
open class DeepCopySymbolRemapper(
    private val descriptorsRemapper: DescriptorsRemapper = NullDescriptorsRemapper
) : IrElementVisitorVoid, SymbolRemapper {

    protected val classes = irInterceptedLocal_hashMapOf<IrClass, IrClassSymbol>()
    protected val scripts = irInterceptedLocal_hashMapOf<IrScript, IrScriptSymbol>()
    protected val constructors = irInterceptedLocal_hashMapOf<IrConstructor, IrConstructorSymbol>()
    protected val enumEntries = irInterceptedLocal_hashMapOf<IrEnumEntry, IrEnumEntrySymbol>()
    protected val externalPackageFragments = irInterceptedLocal_hashMapOf<IrExternalPackageFragment, IrExternalPackageFragmentSymbol>()
    protected val fields = irInterceptedLocal_hashMapOf<IrField, IrFieldSymbol>()
    protected val files = irInterceptedLocal_hashMapOf<IrFile, IrFileSymbol>()
    protected val functions = irInterceptedLocal_hashMapOf<IrSimpleFunction, IrSimpleFunctionSymbol>()
    protected val properties = irInterceptedLocal_hashMapOf<IrProperty, IrPropertySymbol>()
    protected val returnableBlocks = irInterceptedLocal_hashMapOf<IrReturnableBlock, IrReturnableBlockSymbol>()
    protected val typeParameters = irInterceptedLocal_hashMapOf<IrTypeParameter, IrTypeParameterSymbol>()
    protected val valueParameters = irInterceptedLocal_hashMapOf<IrValueParameter, IrValueParameterSymbol>()
    protected val variables = irInterceptedLocal_hashMapOf<IrVariable, IrVariableSymbol>()
    protected val localDelegatedProperties = irInterceptedLocal_hashMapOf<IrLocalDelegatedProperty, IrLocalDelegatedPropertySymbol>()
    protected val typeAliases = irInterceptedLocal_hashMapOf<IrTypeAlias, IrTypeAliasSymbol>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    protected inline fun <D : DeclarationDescriptor, B : IrSymbolOwner, reified S : IrBindableSymbol<D, B>>
            remapSymbol(map: MutableMap<B, S>, owner: B, createNewSymbol: (S) -> S) {
        val symbol = owner.symbol as S
        map[owner] = createNewSymbol(symbol)
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

    override fun visitBlock(expression: IrBlock) {
        if (expression is IrReturnableBlock) {
            remapSymbol(returnableBlocks, expression) {
                IrReturnableBlockSymbolImpl()
            }
        }
        super.visitBlock(expression)
    }

    private fun <B : IrSymbolOwner, S : IrBindableSymbol<*, B>> Map<B, S>.getDeclared(symbol: S) =
        getOrElse(symbol.owner) {
            throw IllegalArgumentException("Non-remapped symbol $symbol")
        }

    private fun <B : IrSymbolOwner, S : IrBindableSymbol<*, B>> Map<B, S>.getReferenced(symbol: S) =
        getOrElse(symbol.owner) { symbol }

    override fun getDeclaredClass(symbol: IrClassSymbol): IrClassSymbol = classes.getDeclared(symbol)
    override fun getDeclaredScript(symbol: IrScriptSymbol): IrScriptSymbol = scripts.getDeclared(symbol)
    override fun getDeclaredFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = functions.getDeclared(symbol)
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

    override fun getReferencedClass(symbol: IrClassSymbol): IrClassSymbol = classes.getReferenced(symbol)
    override fun getReferencedScript(symbol: IrScriptSymbol): IrScriptSymbol = scripts.getReferenced(symbol)
    override fun getReferencedClassOrNull(symbol: IrClassSymbol?): IrClassSymbol? = symbol?.let { classes.getReferenced(it) }
    override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = enumEntries.getReferenced(symbol)
    override fun getReferencedVariable(symbol: IrVariableSymbol): IrVariableSymbol = variables.getReferenced(symbol)
    override fun getReferencedLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol): IrLocalDelegatedPropertySymbol =
        localDelegatedProperties.getReferenced(symbol)

    override fun getReferencedField(symbol: IrFieldSymbol): IrFieldSymbol = fields.getReferenced(symbol)
    override fun getReferencedConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = constructors.getReferenced(symbol)
    override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = functions.getReferenced(symbol)
    override fun getReferencedProperty(symbol: IrPropertySymbol): IrPropertySymbol = properties.getReferenced(symbol)
    override fun getReferencedValue(symbol: IrValueSymbol): IrValueSymbol =
        when (symbol) {
            is IrValueParameterSymbol -> valueParameters.getReferenced(symbol)
            is IrVariableSymbol -> variables.getReferenced(symbol)
        }

    override fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol =
        when (symbol) {
            is IrSimpleFunctionSymbol -> functions.getReferenced(symbol)
            is IrConstructorSymbol -> constructors.getReferenced(symbol)
        }

    override fun getReferencedReturnableBlock(symbol: IrReturnableBlockSymbol): IrReturnableBlockSymbol =
        returnableBlocks.getReferenced(symbol)

    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        when (symbol) {
            is IrClassSymbol -> classes.getReferenced(symbol)
            is IrScriptSymbol -> scripts.getReferenced(symbol)
            is IrTypeParameterSymbol -> typeParameters.getReferenced(symbol)
        }

    override fun getReferencedTypeAlias(symbol: IrTypeAliasSymbol): IrTypeAliasSymbol = typeAliases.getReferenced(symbol)
}