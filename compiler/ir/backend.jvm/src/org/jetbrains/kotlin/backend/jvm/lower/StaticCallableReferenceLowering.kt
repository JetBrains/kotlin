/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

internal val staticCallableReferencePhase = makeIrFilePhase(
    ::StaticCallableReferenceLowering,
    name = "StaticCallableReferencePhase",
    description = "Turn static callable references into singletons"
)

class StaticCallableReferenceLowering(val backendContext: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    private val staticInstanceFields = HashMap<IrClass, IrField>()

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildrenVoid()
        if (declaration.isSyntheticSingleton) {
            declaration.declarations += getFieldForStaticCallableReferenceInstance(declaration).also { field ->
                field.initializer = backendContext.createIrBuilder(field.symbol).run {
                    irExprBody(irCall(declaration.primaryConstructor!!))
                }
            }
        }
        return declaration
    }

    private fun getFieldForStaticCallableReferenceInstance(irClass: IrClass): IrField =
        staticInstanceFields.getOrPut(irClass) {
            backendContext.irFactory.buildField {
                name = Name.identifier(JvmAbi.INSTANCE_FIELD)
                type = irClass.defaultType
                origin = JvmLoweredDeclarationOrigin.FIELD_FOR_STATIC_CALLABLE_REFERENCE_INSTANCE
                isFinal = true
                isStatic = true
                visibility = DescriptorVisibilities.PUBLIC
            }.apply {
                parent = irClass
            }
        }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val constructor = expression.symbol.owner
        if (!constructor.constructedClass.isSyntheticSingleton)
            return super.visitConstructorCall(expression)

        val instanceField = getFieldForStaticCallableReferenceInstance(constructor.constructedClass)
        return IrGetFieldImpl(expression.startOffset, expression.endOffset, instanceField.symbol, expression.type)
    }

    // Recognize callable references with no value or type arguments. The only type arguments in Kotlin stem from usages of
    // reified type parameters, which we unfortunately don't record as parameters so we have to check the body of the class.
    private val IrClass.isSyntheticSingleton: Boolean
        get() = (origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL
                || origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                || origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE)
                && primaryConstructor!!.valueParameters.isEmpty()
                && !containsReifiedTypeParameters

    // Check whether there is any usage of reified type parameters in the body of the given class. This method does not
    // distinguish between reified type parameters declared in inline functions inside the class and those coming from the
    // outside. This is sufficient, because we only apply this function to callable references where this does not matter.
    private val IrClass.containsReifiedTypeParameters: Boolean
        get() = containsReifiedTypeParametersCache.getOrPut(this) {
            var containsReified = false
            acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    if (!containsReified)
                        element.acceptChildrenVoid(this)
                }

                override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
                    for (i in 0 until expression.typeArgumentsCount) {
                        if (expression.getTypeArgument(i)?.isReified == true) {
                            containsReified = true
                            break
                        }
                    }
                    super.visitMemberAccess(expression)
                }

                override fun visitTypeOperator(expression: IrTypeOperatorCall) {
                    if (expression.typeOperand.isReified)
                        containsReified = true
                    super.visitTypeOperator(expression)
                }

                override fun visitClassReference(expression: IrClassReference) {
                    if (expression.classType.isReified)
                        containsReified = true
                    super.visitClassReference(expression)
                }

                private val IrType.isReified: Boolean
                    get() = classifierOrNull?.safeAs<IrTypeParameterSymbol>()?.owner?.isReified == true
            })
            return containsReified
        }

    private val containsReifiedTypeParametersCache = mutableMapOf<IrClass, Boolean>()
}
