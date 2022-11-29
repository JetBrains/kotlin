/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageSupport.UnlinkedMarkerTypeHandler
import org.jetbrains.kotlin.backend.common.serialization.unlinked.UsedClassifierSymbolStatus.*
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class PartialLinkageSupportImpl(private val builtIns: IrBuiltIns) : PartialLinkageSupport {
    private val handler = object : UnlinkedMarkerTypeHandler {
        override val unlinkedMarkerType = IrSimpleTypeImpl(
            classifier = builtIns.anyClass,
            hasQuestionMark = true,
            arguments = emptyList(),
            annotations = emptyList()
        )

        override fun IrType.isUnlinkedMarkerType(): Boolean = this === unlinkedMarkerType
    }

    private val usedClassifierSymbols = UsedClassifierSymbols()

    override val partialLinkageEnabled get() = true

    override fun markUsedClassifiersExcludingUnlinkedFromFakeOverrideBuilding(fakeOverrideBuilder: FakeOverrideBuilder) {
        val entries = fakeOverrideBuilder.fakeOverrideCandidates
        if (entries.isEmpty()) return

        val toRemove = hashSetOf<IrClass>()
        for (clazz in entries.keys) {
            if (clazz.symbol.isUnlinkedClassifier(visited = hashSetOf())) {
                toRemove += clazz
            }
        }

        entries -= toRemove
    }

    private fun IrType.isUnlinkedType(visited: MutableSet<IrClassifierSymbol>): Boolean {
        val simpleType = this as? IrSimpleType ?: return this !is IrErrorType

        if (simpleType.classifier.isUnlinkedClassifier(visited))
            return true

        for (argument in simpleType.arguments) {
            if (argument is IrTypeProjection) {
                if (argument.type.isUnlinkedType(visited))
                    return true
            }
        }

        return false
    }

    private fun IrClassifierSymbol.isUnlinkedClassifier(visited: MutableSet<IrClassifierSymbol>): Boolean {
        when (val status = usedClassifierSymbols[this]) {
            UNLINKED, LINKED -> return status.isUnlinked
            null -> {
                // Unknown classifier. Continue.
            }
        }

        if (!isBound || (hasDescriptor && descriptor is NotFoundClasses.MockClassDescriptor))
            return usedClassifierSymbols.register(this, UNLINKED)

        if (!visited.add(this))
            return false // Recursion avoidance.

        when (val classifier = owner) {
            is IrClass -> {
                if (classifier.parentClassOrNull?.symbol?.isUnlinkedClassifier(visited) == true)
                    return usedClassifierSymbols.register(this, UNLINKED)

                for (typeParameter in classifier.typeParameters) {
                    if (typeParameter.superTypes.any { it.isUnlinkedType(visited) })
                        return usedClassifierSymbols.register(this, UNLINKED)
                }

                if (classifier.superTypes.any { it.isUnlinkedType(visited) })
                    return usedClassifierSymbols.register(this, UNLINKED)
            }
            is IrTypeParameter -> {
                if (classifier.superTypes.any { it.isUnlinkedType(visited) })
                    return usedClassifierSymbols.register(this, UNLINKED)
            }
        }

        return usedClassifierSymbols.register(this, LINKED)
    }

    override fun markUsedClassifiersInInlineLazyIrFunction(function: IrFunction) {
        function.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            fun visitType(type: IrType?) {
                type?.isUnlinkedType(visited = hashSetOf())
            }

            override fun visitValueParameter(declaration: IrValueParameter) {
                visitType(declaration.type)
                super.visitValueParameter(declaration)
            }

            override fun visitTypeParameter(declaration: IrTypeParameter) {
                declaration.superTypes.forEach(::visitType)
                super.visitTypeParameter(declaration)
            }

            override fun visitFunction(declaration: IrFunction) {
                visitType(declaration.returnType)
                super.visitFunction(declaration)
            }

            override fun visitField(declaration: IrField) {
                visitType(declaration.type)
                super.visitField(declaration)
            }

            override fun visitVariable(declaration: IrVariable) {
                visitType(declaration.type)
                super.visitVariable(declaration)
            }

            override fun visitExpression(expression: IrExpression) {
                visitType(expression.type)
                super.visitExpression(expression)
            }

            override fun visitClassReference(expression: IrClassReference) {
                visitType(expression.classType)
                super.visitClassReference(expression)
            }

            override fun visitConstantObject(expression: IrConstantObject) {
                expression.typeArguments.forEach(::visitType)
                super.visitConstantObject(expression)
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall) {
                visitType(expression.typeOperand)
                super.visitTypeOperator(expression)
            }
        })
    }

    override fun processUnlinkedDeclarations(messageLogger: IrMessageLogger, lazyRoots: () -> List<IrElement>) {
        val processor = UnlinkedDeclarationsProcessor(builtIns, usedClassifierSymbols, handler, messageLogger)
        processor.addLinkageErrorIntoUnlinkedClasses()

        val roots = lazyRoots()

        val signatureTransformer = processor.signatureTransformer()
        roots.forEach { it.transformChildrenVoid(signatureTransformer) }

        val usageTransformer = processor.usageTransformer()
        roots.forEach { it.transformChildrenVoid(usageTransformer) }
    }
}
