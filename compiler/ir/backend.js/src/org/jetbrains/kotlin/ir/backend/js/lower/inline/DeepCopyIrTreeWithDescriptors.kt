/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

internal class DeepCopyIrTreeWithSymbolsForInliner(val context: Context,
                                                   val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
                                                   val parent: IrDeclarationParent?) {

    fun copy(irElement: IrElement): IrElement {
        // Create new symbols.
        irElement.acceptVoid(symbolRemapper)

        // Make symbol remapper aware of the callsite's type arguments.
        symbolRemapper.typeArguments = typeArguments

        // Copy IR.
        val result = irElement.transform(copier, data = null)

        // Bind newly created IR with wrapped descriptors.
        result.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                (declaration.descriptor as WrappedClassDescriptor).bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                (declaration.descriptor as WrappedClassConstructorDescriptor).bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitEnumEntry(declaration: IrEnumEntry) {
                (declaration.descriptor as WrappedClassDescriptor).bind(
                        declaration.correspondingClass ?: declaration.parentAsClass)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitField(declaration: IrField) {
                (declaration.descriptor as? WrappedFieldDescriptor)?.bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                (declaration.descriptor as WrappedSimpleFunctionDescriptor).bind(declaration as IrSimpleFunction)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitValueParameter(declaration: IrValueParameter) {
                (declaration.descriptor as? WrappedValueParameterDescriptor)?.bind(declaration)
                (declaration.descriptor as? WrappedReceiverParameterDescriptor)?.bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitTypeParameter(declaration: IrTypeParameter) {
                (declaration.descriptor as WrappedTypeParameterDescriptor).bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitVariable(declaration: IrVariable) {
                (declaration.descriptor as WrappedVariableDescriptor).bind(declaration)
                declaration.acceptChildrenVoid(this)
            }
        })

        result.patchDeclarationParents(parent)
        return result
    }

    private var nameIndex = 0

    private fun generateCopyName(name: Name) = Name.identifier(name.toString() + "_" + (nameIndex++).toString())

    private inner class InlinerSymbolRenamer : SymbolRenamer {
        private val map = mutableMapOf<IrSymbol, Name>()

        override fun getClassName(symbol: IrClassSymbol) = map.getOrPut(symbol) { generateCopyName(symbol.owner.name) }
        override fun getFunctionName(symbol: IrSimpleFunctionSymbol) = map.getOrPut(symbol) { generateCopyName(symbol.owner.name) }
        override fun getFieldName(symbol: IrFieldSymbol) = symbol.owner.name
        override fun getFileName(symbol: IrFileSymbol) = symbol.owner.fqName
        override fun getExternalPackageFragmentName(symbol: IrExternalPackageFragmentSymbol) = symbol.owner.fqName
        override fun getEnumEntryName(symbol: IrEnumEntrySymbol) = symbol.owner.name
        override fun getVariableName(symbol: IrVariableSymbol) = map.getOrPut(symbol) { generateCopyName(symbol.owner.name) }
        override fun getTypeParameterName(symbol: IrTypeParameterSymbol) = symbol.owner.name
        override fun getValueParameterName(symbol: IrValueParameterSymbol) = symbol.owner.name
    }

    private inner class DescriptorsToIrRemapper : DescriptorsRemapper {
        override fun remapDeclaredClass(descriptor: ClassDescriptor) =
                WrappedClassDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredConstructor(descriptor: ClassConstructorDescriptor) =
                WrappedClassConstructorDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredEnumEntry(descriptor: ClassDescriptor) =
                WrappedClassDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredField(descriptor: PropertyDescriptor) =
                WrappedFieldDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredSimpleFunction(descriptor: FunctionDescriptor) =
                WrappedSimpleFunctionDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredTypeParameter(descriptor: TypeParameterDescriptor) =
                WrappedTypeParameterDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredVariable(descriptor: VariableDescriptor) =
                WrappedVariableDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredValueParameter(descriptor: ParameterDescriptor): ParameterDescriptor =
                if (descriptor is ReceiverParameterDescriptor)
                    WrappedReceiverParameterDescriptor(descriptor.annotations, descriptor.source)
                else
                    WrappedValueParameterDescriptor(descriptor.annotations, descriptor.source)
    }

    private inner class InlinerTypeRemapper(val symbolRemapper: SymbolRemapper,
                                            val typeArguments: Map<IrTypeParameterSymbol, IrType?>?) : TypeRemapper {

        override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) { }

        override fun leaveScope() { }

        private fun remapTypeArguments(arguments: List<IrTypeArgument>) =
                arguments.map { argument ->
                    (argument as? IrTypeProjection)?.let { makeTypeProjection(remapType(it.type), it.variance) }
                            ?: argument
                }

        override fun remapType(type: IrType): IrType {
            if (type !is IrSimpleType) return type

            val substitutedType = typeArguments?.get(type.classifier)
            if (substitutedType != null) {
                substitutedType as IrSimpleType
                return IrSimpleTypeImpl(
                        kotlinType      = null,
                        classifier      = substitutedType.classifier,
                        hasQuestionMark = type.hasQuestionMark or substitutedType.isMarkedNullable(),
                        arguments       = substitutedType.arguments,
                        annotations     = substitutedType.annotations
                )
            }

            return IrSimpleTypeImpl(
                    kotlinType      = null,
                    classifier      = symbolRemapper.getReferencedClassifier(type.classifier),
                    hasQuestionMark = type.hasQuestionMark,
                    arguments       = remapTypeArguments(type.arguments),
                    annotations     = type.annotations.map { it.transform(copier, null) as IrCall }
            )
        }
    }

    private class SymbolRemapperImpl(descriptorsRemapper: DescriptorsRemapper)
        : DeepCopySymbolRemapper(descriptorsRemapper) {

        var typeArguments: Map<IrTypeParameterSymbol, IrType?>? = null
            set(value) {
                if (field != null) return
                field = value?.asSequence()?.associate {
                    (getReferencedClassifier(it.key) as IrTypeParameterSymbol) to it.value
                }
            }

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
            val result = super.getReferencedClassifier(symbol)
            if (result !is IrTypeParameterSymbol)
                return result
            return typeArguments?.get(result)?.classifierOrNull ?: result
        }
    }

    private val symbolRemapper = SymbolRemapperImpl(DescriptorsToIrRemapper())
    private val copier = DeepCopyIrTreeWithSymbols(
            symbolRemapper,
            InlinerTypeRemapper(symbolRemapper, typeArguments),
            InlinerSymbolRenamer()
    )
}