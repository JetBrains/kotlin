/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.isInPublicInlineScope
import org.jetbrains.kotlin.backend.jvm.ir.javaClassReference
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal val annotationImplementationPhase = makeIrFilePhase<JvmBackendContext>(
    { ctxt -> AnnotationImplementationLowering { JvmAnnotationImplementationTransformer(ctxt, it) } },
    name = "AnnotationImplementation",
    description = "Create synthetic annotations implementations and use them in annotations constructor calls"
)

class JvmAnnotationImplementationTransformer(val jvmContext: JvmBackendContext, file: IrFile) :
    AnnotationImplementationTransformer(jvmContext, file) {
    private val publicAnnotationImplementationClasses = mutableSetOf<IrClassSymbol>()

    // FIXME: Copied from JvmSingleAbstractMethodLowering
    private val inInlineFunctionScope: Boolean
        get() = allScopes.any { it.irElement.safeAs<IrDeclaration>()?.isInPublicInlineScope == true }

    @Suppress("UNUSED_PARAMETER")
    override fun chooseConstructor(implClass: IrClass, expression: IrConstructorCall) =
        implClass.constructors.single()

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val constructedClass = expression.type.classOrNull
        if (constructedClass?.owner?.isAnnotationClass == true && inInlineFunctionScope) {
            publicAnnotationImplementationClasses += constructedClass
        }
        return super.visitConstructorCall(expression)
    }

    private fun IrType.kClassToJClassIfNeeded(): IrType = when {
        this.isKClass() -> jvmContext.ir.symbols.javaLangClass.starProjectedType
        this.isKClassArray() -> jvmContext.irBuiltIns.arrayClass.typeWith(
            jvmContext.ir.symbols.javaLangClass.starProjectedType
        )
        else -> this
    }

    private fun IrType.isKClassArray() =
        this is IrSimpleType && isArray() && arguments.single().typeOrNull?.isKClass() == true

    private fun IrBuilderWithScope.kClassToJClass(irExpression: IrExpression): IrExpression =
        irGet(
            jvmContext.ir.symbols.javaLangClass.starProjectedType,
            null,
            jvmContext.ir.symbols.kClassJava.owner.getter!!.symbol
        ).apply {
            extensionReceiver = irExpression
        }

    /**
     * Copies array by one element, roughly as following:
     *     val size = kClassArray.size
     *     val result = arrayOfNulls<java.lang.Class>(size)
     *     var i = 0
     *     while(i < size) {
     *         result[i] = kClassArray[i].java
     *         i++
     *     }
     * Partially taken from ArrayConstructorLowering.kt
     */
    private fun IrBuilderWithScope.kClassArrayToJClassArray(kClassArray: IrExpression): IrExpression {
        val javaLangClass = jvmContext.ir.symbols.javaLangClass.starProjectedType
        val jlcArray = jvmContext.ir.symbols.array.typeWith(javaLangClass)
        val arrayClass = jvmContext.ir.symbols.array.owner
        val arrayOfNulls = jvmContext.ir.symbols.arrayOfNulls
        val arraySizeSymbol = arrayClass.findDeclaration<IrProperty> { it.name.asString() == "size" }!!.getter!!

        val block = irBlock {
            val sourceArray = createTmpVariable(kClassArray, "src", isMutable = false)
            val index = createTmpVariable(irInt(0), "i", isMutable = true)
            val size = createTmpVariable(
                irCall(arraySizeSymbol).apply { dispatchReceiver = irGet(sourceArray) },
                "size", isMutable = false
            )
            val result = createTmpVariable(irCall(arrayOfNulls, jlcArray).apply {
                listOf(javaLangClass)
                putValueArgument(0, irGet(size))
            })
            val comparison = primitiveOp2(
                startOffset, endOffset,
                context.irBuiltIns.lessFunByOperandType[context.irBuiltIns.intType.classifierOrFail]!!,
                context.irBuiltIns.booleanType,
                IrStatementOrigin.LT,
                irGet(index), irGet(size)
            )
            val setArraySymbol = arrayClass.functions.single { it.name == OperatorNameConventions.SET }
            val getArraySymbol = arrayClass.functions.single { it.name == OperatorNameConventions.GET }
            val inc = context.irBuiltIns.intType.getClass()!!.functions.single { it.name == OperatorNameConventions.INC }
            +irWhile().also { loop ->
                loop.condition = comparison
                loop.body = irBlock {
                    val tempIndex = createTmpVariable(irGet(index))

                    val getArray = irCall(getArraySymbol).apply {
                        dispatchReceiver = irGet(sourceArray)
                        putValueArgument(0, irGet(tempIndex))
                    }
                    +irCall(setArraySymbol).apply {
                        dispatchReceiver = irGet(result)
                        putValueArgument(0, irGet(tempIndex))
                        putValueArgument(1, kClassToJClass(getArray))
                    }

                    +irSet(index.symbol, irCallOp(inc.symbol, index.type, irGet(index)))
                }
            }
            +irGet(result)
        }
        return block
    }

    override fun getArrayContentEqualsSymbol(type: IrType): IrFunctionSymbol {
        val targetType = if (type.isPrimitiveArray()) type else jvmContext.ir.symbols.arrayOfAnyNType
        val requiredSymbol = jvmContext.ir.symbols.arraysClass.owner.findDeclaration<IrFunction> {
            it.name.asString() == "equals" && it.valueParameters.size == 2 && it.valueParameters.first().type == targetType
        }
        requireNotNull(requiredSymbol) { "Can't find Arrays.equals method for type ${targetType.render()}" }
        return requiredSymbol.symbol
    }

    override fun implementPlatformSpecificParts(annotationClass: IrClass, implClass: IrClass) {
        // Mark the implClass as part of the public ABI if it was instantiated from a public
        // inline function, since annotation implementation classes are regenerated during inlining.
        if (annotationClass.symbol in publicAnnotationImplementationClasses) {
            jvmContext.publicAbiSymbols += implClass.symbol
        }

        implClass.addFunction(
            name = "annotationType",
            returnType = jvmContext.ir.symbols.javaLangClass.starProjectedType,
            origin = ANNOTATION_IMPLEMENTATION,
            isStatic = false
        ).apply {
            body = jvmContext.createJvmIrBuilder(symbol).run {
                irBlockBody {
                    +irReturn(javaClassReference(annotationClass.defaultType))
                }
            }
        }
    }

    override fun implementAnnotationPropertiesAndConstructor(
        implClass: IrClass,
        annotationClass: IrClass,
        generatedConstructor: IrConstructor
    ) {
        val ctorBodyBuilder = context.createIrBuilder(generatedConstructor.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
        val ctorBody = context.irFactory.createBlockBody(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, listOf(
                IrDelegatingConstructorCallImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.unitType, context.irBuiltIns.anyClass.constructors.single(),
                    typeArgumentsCount = 0, valueArgumentsCount = 0
                )
            )
        )

        generatedConstructor.body = ctorBody

        annotationClass.getAnnotationProperties().forEach { property ->
            val propType = property.getter!!.returnType
            val storedFieldType = propType.kClassToJClassIfNeeded()
            val propName = property.name
            val field = context.irFactory.buildField {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                name = propName
                type = storedFieldType
                origin = ANNOTATION_IMPLEMENTATION
                isFinal = true
                visibility = DescriptorVisibilities.PRIVATE
            }.also { it.parent = implClass }

            val parameter = generatedConstructor.addValueParameter(propName.asString(), propType)

            val defaultExpression = property.backingField?.initializer?.expression
            val newDefaultValue: IrExpressionBody? =
                if (defaultExpression is IrGetValue && defaultExpression.symbol.owner is IrValueParameter) {
                    // INITIALIZE_PROPERTY_FROM_PARAMETER
                    (defaultExpression.symbol.owner as IrValueParameter).defaultValue
                } else if (defaultExpression != null) {
                    property.backingField!!.initializer
                } else null
            parameter.defaultValue = newDefaultValue?.deepCopyWithVariables()?.also { it.transformChildrenVoid() }

            ctorBody.statements += with(ctorBodyBuilder) {
                val param = irGet(parameter)
                val fieldValue = when {
                    propType.isKClass() -> kClassToJClass(param)
                    propType.isKClassArray() -> kClassArrayToJClassArray(param)
                    else -> param
                }
                irSetField(irGet(implClass.thisReceiver!!), field, fieldValue)
            }

            val prop = implClass.addProperty {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                name = propName
                isVar = false
                origin = ANNOTATION_IMPLEMENTATION
            }.apply {
                field.correspondingPropertySymbol = this.symbol
                backingField = field
                parent = implClass
                overriddenSymbols = listOf(property.symbol)
            }

            prop.addGetter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                name = propName  // Annotation value getter should be named 'x', not 'getX'
                returnType = propType.kClassToJClassIfNeeded() // On JVM, annotation store j.l.Class even if declared with KClass
                origin = ANNOTATION_IMPLEMENTATION
                visibility = DescriptorVisibilities.PUBLIC
                modality = Modality.FINAL
            }.apply {
                correspondingPropertySymbol = prop.symbol
                dispatchReceiverParameter = implClass.thisReceiver!!.copyTo(this)
                body = context.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                    +irReturn(irGetField(irGet(dispatchReceiverParameter!!), field))
                }
                overriddenSymbols = listOf(property.getter!!.symbol)
            }
        }
    }

    override fun generateFunctionBodies(
        annotationClass: IrClass,
        implClass: IrClass,
        eqFun: IrSimpleFunction,
        hcFun: IrSimpleFunction,
        toStringFun: IrSimpleFunction,
        generator: AnnotationImplementationMemberGenerator
    ) {
        val properties = annotationClass.getAnnotationProperties()
        val implProperties = implClass.getAnnotationProperties()
        generator.generateEqualsUsingGetters(eqFun, annotationClass.defaultType, properties)
        generator.generateHashCodeMethod(hcFun, implProperties)
        generator.generateToStringMethod(toStringFun, implProperties)
    }

}
