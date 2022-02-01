/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.hasMangledParameters
import org.jetbrains.kotlin.backend.jvm.ir.getAnnotationRetention
import org.jetbrains.kotlin.backend.jvm.ir.getJvmNameFromAnnotation
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_OVERLOADS_FQ_NAME
import org.jetbrains.kotlin.types.Variance
import java.lang.annotation.ElementType

internal val jvmClassPreprocessingPhase = makeIrFilePhase(
    ::JvmClassPreprocessingLowering,
    name = "JvmClassPreprocessing",
    description = "JVM-specific class members preprocessing"
)


class JvmClassPreprocessingLowering(val context: JvmBackendContext) : ClassLoweringPass {

    private val isExtendedMainConvention =
        context.configuration.languageVersionSettings.supportsFeature(LanguageFeature.ExtendedMainConvention)

    private val javaAnnotations =
        context.ir.symbols.javaAnnotations

    override fun lower(irClass: IrClass) {
        val isFileClass = irClass.isFileClass
        val isEligibleForNoArgConstructor = irClass.isEligibleForNoArgConstructor()


        if (irClass.isAnnotationClass) {
            // Remove constructors of annotation classes
            irClass.declarations.removeIf { it is IrConstructor }

            // Generate JVM-specific annotations (@Documented, @Retention, @Target, @Repeatable)
            generateDocumentedAnnotation(irClass)
            generateRetentionAnnotation(irClass)
            generateTargetAnnotation(irClass)
            generateRepeatableAnnotation(irClass)
        }

        for (i in irClass.declarations.indices) {
            when (val member = irClass.declarations[i]) {
                is IrTypeAlias -> {
                    val irTypeAlias: IrTypeAlias = member

                    // Generate synthetic method for typealias annotations
                    if (irTypeAlias.annotations.isNotEmpty()) {
                        generateAnnotationsMethodForTypeAlias(irClass, irTypeAlias)
                    }
                }
                is IrFunction -> {
                    val irFun: IrFunction = member

                    // Generate wrappers for member functions with @JvmDefault annotation
                    if (irFun.hasJvmOverloadsAnnotation()) {
                        generateJvmDefaultWrappers(irClass, irFun)
                    }

                    // Generate wrappers for extended main convention
                    if (isFileClass && isExtendedMainConvention && irFun is IrSimpleFunction) {
                        val irSimpleFun: IrSimpleFunction = irFun
                        if (irSimpleFun.isMainMethod() && irSimpleFun.isSuspend) {
                            generateSuspendMainWrapper(irClass, irSimpleFun)
                        } else if (irSimpleFun.isParameterlessMainMethod()) {
                            generateParameterlessMainWrapper(irClass, irSimpleFun)
                        }
                    }

                    // Generate no-arg constructor if required
                    if (irFun is IrConstructor) {
                        val irConstructor: IrConstructor = irFun
                        if (isEligibleForNoArgConstructor && irConstructor.shouldGenerateNoArgConstructor()) {
                            generateNoArgConstructor(irClass, irConstructor)
                        }
                    }
                }

            }
        }
    }

    private fun IrClass.isEligibleForNoArgConstructor(): Boolean =
        this.kind == ClassKind.CLASS &&
                this.visibility != DescriptorVisibilities.LOCAL &&
                this.modality != Modality.SEALED &&
                !this.isSingleFieldValueClass &&
                !this.isInner

    private fun IrConstructor.shouldGenerateNoArgConstructor(): Boolean =
        this.isPrimary &&
                !DescriptorVisibilities.isPrivate(this.visibility) &&
                this.valueParameters.isNotEmpty() &&
                !this.hasMangledParameters &&
                this.allParametersHaveDefaultValues() &&
                this.parentAsClass.constructors.none { it.conflictsWithNoArgConstructor() }

    private fun IrFunction.hasJvmOverloadsAnnotation() =
        this.hasAnnotation(JVM_OVERLOADS_FQ_NAME)

    private fun IrConstructor.conflictsWithNoArgConstructor() =
        !this.isPrimary && (this.valueParameters.isEmpty() || this.allParametersHaveDefaultValues())

    private fun IrConstructor.allParametersHaveDefaultValues() =
        this.valueParameters.all { it.defaultValue != null }

    private fun IrSimpleFunction.isParameterlessMainMethod(): Boolean =
        typeParameters.isEmpty() &&
                extensionReceiverParameter == null &&
                valueParameters.isEmpty() &&
                returnType.isUnit() &&
                name.asString() == "main"

    private fun IrSimpleFunction.isMainMethod(): Boolean {
        if ((getJvmNameFromAnnotation() ?: name.asString()) != "main") return false
        if (!returnType.isUnit()) return false

        val parameter = allParameters.singleOrNull() ?: return false
        if (!parameter.type.isArray() && !parameter.type.isNullableArray()) return false

        val argType = (parameter.type as IrSimpleType).arguments.first() as? IrTypeProjection
            ?: return false
        return (argType.variance != Variance.IN_VARIANCE) && argType.type.isStringClassType()
    }


    private fun generateAnnotationsMethodForTypeAlias(irClass: IrClass, irTypeAlias: IrTypeAlias) {
        irClass.addFunction {
            name = irTypeAlias.syntheticAnnotationMethodName
            visibility = irTypeAlias.visibility
            returnType = context.irBuiltIns.unitType
            modality = Modality.OPEN
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS
        }.apply {
            body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            annotations = irTypeAlias.annotations
        }
    }

    private val IrTypeAlias.syntheticAnnotationMethodName
        get() = Name.identifier(JvmAbi.getSyntheticMethodNameForAnnotatedTypeAlias(name))

    private fun generateJvmDefaultWrappers(irClass: IrClass, irFunction: IrFunction) {
        val numDefaultParameters = irFunction.valueParameters.count { it.defaultValue != null }
        for (i in numDefaultParameters - 1 downTo 0) {
            irClass.addMember(generateJvmOverloadsWrapper(irFunction, i))
        }
    }

    private fun generateJvmOverloadsWrapper(target: IrFunction, numDefaultParametersToExpect: Int): IrFunction {
        val wrapperIrFunction = context.irFactory.generateJvmOverloadsWrapperHeader(target, numDefaultParametersToExpect)

        val call = when (target) {
            is IrConstructor ->
                IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, target.symbol
                )
            is IrSimpleFunction ->
                IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target.returnType, target.symbol)
            else ->
                error("unknown function kind: ${target.render()}")
        }
        for (arg in wrapperIrFunction.allTypeParameters) {
            call.putTypeArgument(arg.index, arg.defaultType)
        }
        call.dispatchReceiver = wrapperIrFunction.dispatchReceiverParameter?.let { dispatchReceiver ->
            IrGetValueImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                dispatchReceiver.symbol
            )
        }
        call.extensionReceiver = wrapperIrFunction.extensionReceiverParameter?.let { extensionReceiver ->
            IrGetValueImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                extensionReceiver.symbol
            )
        }

        var parametersCopied = 0
        var defaultParametersCopied = 0
        for ((i, valueParameter) in target.valueParameters.withIndex()) {
            if (valueParameter.defaultValue != null) {
                if (defaultParametersCopied < numDefaultParametersToExpect) {
                    defaultParametersCopied++
                    call.putValueArgument(
                        i,
                        IrGetValueImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            wrapperIrFunction.valueParameters[parametersCopied++].symbol
                        )
                    )
                } else {
                    call.putValueArgument(i, null)
                }
            } else {
                call.putValueArgument(
                    i,
                    IrGetValueImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        wrapperIrFunction.valueParameters[parametersCopied++].symbol
                    )
                )
            }

        }

        wrapperIrFunction.body = if (target is IrConstructor) {
            IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(call))
        } else {
            IrExpressionBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, call
            )
        }

        return wrapperIrFunction
    }

    private fun IrFactory.generateJvmOverloadsWrapperHeader(oldFunction: IrFunction, numDefaultParametersToExpect: Int): IrFunction {
        val res = when (oldFunction) {
            is IrConstructor -> {
                buildConstructor {
                    origin = JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER
                    name = oldFunction.name
                    visibility = oldFunction.visibility
                    returnType = oldFunction.returnType
                    isInline = oldFunction.isInline
                }
            }
            is IrSimpleFunction -> buildFun {
                origin = JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER
                name = oldFunction.name
                visibility = oldFunction.visibility
                modality =
                    if (context.state.languageVersionSettings.supportsFeature(LanguageFeature.GenerateJvmOverloadsAsFinal)) Modality.FINAL
                    else oldFunction.modality
                returnType = oldFunction.returnType
                isInline = oldFunction.isInline
                isSuspend = oldFunction.isSuspend
            }
            else -> error("Unknown kind of IrFunction: $oldFunction")
        }

        res.parent = oldFunction.parent
        res.copyAnnotationsFrom(oldFunction)
        res.copyTypeParametersFrom(oldFunction)
        res.dispatchReceiverParameter = oldFunction.dispatchReceiverParameter?.copyTo(res)
        res.extensionReceiverParameter = oldFunction.extensionReceiverParameter?.copyTo(res)
        res.valueParameters += res.generateJvmOverloadsWrapperValueParameters(oldFunction, numDefaultParametersToExpect)
        return res
    }

    private fun IrFunction.generateJvmOverloadsWrapperValueParameters(
        oldFunction: IrFunction,
        numDefaultParametersToExpect: Int
    ): List<IrValueParameter> {
        var parametersCopied = 0
        var defaultParametersCopied = 0
        val result = mutableListOf<IrValueParameter>()
        for (oldValueParameter in oldFunction.valueParameters) {
            if (oldValueParameter.defaultValue != null &&
                defaultParametersCopied < numDefaultParametersToExpect
            ) {
                defaultParametersCopied++
                result.add(
                    oldValueParameter.copyTo(
                        this,
                        index = parametersCopied++,
                        defaultValue = null,
                        isCrossinline = oldValueParameter.isCrossinline,
                        isNoinline = oldValueParameter.isNoinline
                    )
                )
            } else if (oldValueParameter.defaultValue == null) {
                result.add(oldValueParameter.copyTo(this, index = parametersCopied++))
            }
        }
        return result
    }

    private fun IrClass.generateMainMethod(makeBody: IrBlockBodyBuilder.(IrSimpleFunction, IrValueParameter) -> Unit) =
        addFunction {
            name = Name.identifier("main")
            visibility = DescriptorVisibilities.PUBLIC
            returnType = context.irBuiltIns.unitType
            modality = Modality.OPEN
            origin = JvmLoweredDeclarationOrigin.GENERATED_EXTENDED_MAIN
        }.apply {
            val args = addValueParameter {
                name = Name.identifier("args")
                type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)
            }
            body = context.createIrBuilder(symbol).irBlockBody { makeBody(this@apply, args) }
        }

    private fun IrBuilderWithScope.irRunSuspend(
        target: IrSimpleFunction,
        args: IrValueParameter?,
        newMain: IrSimpleFunction
    ): IrExpression {
        val backendContext = this@JvmClassPreprocessingLowering.context
        return irBlock {
            val wrapperConstructor = backendContext.irFactory.buildClass {
                name = Name.special("<main-wrapper>")
                visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                modality = Modality.FINAL
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            }.let { wrapper ->
                +wrapper

                wrapper.createImplicitParameterDeclarationWithWrappedDescriptor()

                val lambdaSuperClass = backendContext.ir.symbols.lambdaClass
                val functionClass = backendContext.ir.symbols.getJvmSuspendFunctionClass(0)

                wrapper.superTypes += lambdaSuperClass.defaultType
                wrapper.superTypes += functionClass.typeWith(backendContext.irBuiltIns.anyNType)
                wrapper.parent = newMain

                val stringArrayType = backendContext.irBuiltIns.arrayClass.typeWith(backendContext.irBuiltIns.stringType)
                val argsField = args?.let {
                    wrapper.addField {
                        name = Name.identifier("args")
                        type = stringArrayType
                        visibility = DescriptorVisibilities.PRIVATE
                        origin = LocalDeclarationsLowering.DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE
                    }
                }

                wrapper.addFunction("invoke", backendContext.irBuiltIns.anyNType, isSuspend = true).also { invoke ->
                    val invokeToOverride = functionClass.functions.single()

                    invoke.overriddenSymbols += invokeToOverride
                    invoke.body = backendContext.createIrBuilder(invoke.symbol).irBlockBody {
                        +irReturn(irCall(target.symbol).also { call ->
                            if (args != null) {
                                call.putValueArgument(0, irGetField(irGet(invoke.dispatchReceiverParameter!!), argsField!!))
                            }
                        })
                    }
                }

                wrapper.addConstructor {
                    isPrimary = true
                    visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                }.also { constructor ->
                    val superClassConstructor = lambdaSuperClass.owner.constructors.single()
                    val param = args?.let { constructor.addValueParameter("args", stringArrayType) }

                    constructor.body = backendContext.createIrBuilder(constructor.symbol).irBlockBody {
                        +irDelegatingConstructorCall(superClassConstructor).also {
                            it.putValueArgument(0, irInt(1))
                        }
                        if (args != null) {
                            +irSetField(irGet(wrapper.thisReceiver!!), argsField!!, irGet(param!!))
                        }
                    }
                }
            }

            +irCall(backendContext.ir.symbols.runSuspendFunction).apply {
                putValueArgument(
                    0, IrConstructorCallImpl.fromSymbolOwner(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        wrapperConstructor.returnType,
                        wrapperConstructor.symbol
                    ).also {
                        if (args != null) {
                            it.putValueArgument(0, irGet(args))
                        }
                    }
                )
            }
        }
    }

    private fun generateParameterlessMainWrapper(irClass: IrClass, irSimpleFun: IrSimpleFunction) {
        irClass.generateMainMethod { newMain, _ ->
            if (irSimpleFun.isSuspend) {
                +irRunSuspend(irSimpleFun, null, newMain)
            } else {
                +irCall(irSimpleFun)
            }
        }
    }

    private fun generateSuspendMainWrapper(irClass: IrClass, irSimpleFun: IrSimpleFunction) {
        irClass.generateMainMethod { newMain, args ->
            +irRunSuspend(irSimpleFun, args, newMain)
        }
    }

    private fun generateNoArgConstructor(irClass: IrClass, irConstructor: IrConstructor) {
        irClass.addConstructor {
            visibility = irConstructor.visibility
        }.apply {
            val irBuilder = context.createIrBuilder(this.symbol, startOffset, endOffset)
            copyAnnotationsFrom(irConstructor)
            body = irBuilder.irBlockBody {
                +irDelegatingConstructorCall(irConstructor).apply {
                    passTypeArgumentsFrom(irClass)
                    passTypeArgumentsFrom(irConstructor, irClass.typeParameters.size)
                }
            }
        }
    }

    private fun irAnnotationConstructorCall(constructor: IrConstructor) =
        IrConstructorCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            constructor.returnType,
            constructor.symbol,
            0
        )

    private fun irGetEnumValue(irEnumEntry: IrEnumEntry) =
        IrGetEnumValueImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            irEnumEntry.parentAsClass.defaultType,
            irEnumEntry.symbol
        )

    private fun irVararg(elementType: IrType) =
        IrVarargImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            type = context.irBuiltIns.arrayClass.typeWith(elementType),
            varargElementType = elementType
        )

    private fun generateDocumentedAnnotation(irClass: IrClass) {
        if (!irClass.hasAnnotation(StandardNames.FqNames.mustBeDocumented) ||
            irClass.hasAnnotation(JvmAnnotationNames.DOCUMENTED_ANNOTATION)
        ) return

        irClass.annotations += irAnnotationConstructorCall(javaAnnotations.documentedConstructor)
    }

    private fun generateRetentionAnnotation(irClass: IrClass) {
        if (irClass.hasAnnotation(JvmAnnotationNames.RETENTION_ANNOTATION)) return
        val kotlinRetentionPolicy = irClass.getAnnotationRetention()
        val javaRetentionPolicy = kotlinRetentionPolicy?.let { javaAnnotations.annotationRetentionMap[it] } ?: javaAnnotations.rpRuntime

        irClass.annotations +=
            irAnnotationConstructorCall(javaAnnotations.retentionConstructor).apply {
                putValueArgument(0, irGetEnumValue(javaRetentionPolicy))
            }
    }

    private fun generateTargetAnnotation(irClass: IrClass) {
        if (irClass.hasAnnotation(JvmAnnotationNames.TARGET_ANNOTATION)) return
        val annotationTargetMap = javaAnnotations.getAnnotationTargetMap(context.state.target)

        val targets = irClass.applicableTargetSet() ?: return
        val javaTargets = targets.mapNotNullTo(HashSet()) { annotationTargetMap[it] }.sortedBy {
            ElementType.valueOf(it.symbol.owner.name.asString())
        }

        val vararg = irVararg(javaAnnotations.elementTypeEnum.defaultType)
        for (target in javaTargets) {
            vararg.elements.add(irGetEnumValue(target))
        }

        irClass.annotations +=
            irAnnotationConstructorCall(javaAnnotations.targetConstructor).apply {
                putValueArgument(0, vararg)
            }
    }

    private fun generateRepeatableAnnotation(irClass: IrClass) {
        if (!irClass.hasAnnotation(StandardNames.FqNames.repeatable) ||
            irClass.hasAnnotation(JvmAnnotationNames.REPEATABLE_ANNOTATION)
        ) return

        val containerClass =
            irClass.declarations.singleOrNull {
                it is IrClass && it.name.asString() == JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME
            } as IrClass? ?: error("Repeatable annotation class should have a container generated: ${irClass.render()}")
        val containerReference = IrClassReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.kClassClass.typeWith(containerClass.defaultType),
            containerClass.symbol, containerClass.defaultType
        )
        irClass.annotations +=
            irAnnotationConstructorCall(javaAnnotations.repeatableConstructor).apply {
                putValueArgument(0, containerReference)
            }
    }

    private fun IrConstructorCall.getValueArgument(name: Name): IrExpression? {
        val index = symbol.owner.valueParameters.find { it.name == name }?.index
            ?: return null
        return getValueArgument(index)
    }

    private fun IrClass.applicableTargetSet(): Set<KotlinTarget>? {
        val targetEntry = getAnnotation(StandardNames.FqNames.target)
            ?: return null
        return loadAnnotationTargets(targetEntry)
    }

    private fun loadAnnotationTargets(targetEntry: IrConstructorCall): Set<KotlinTarget>? {
        val valueArgument = targetEntry.getValueArgument(Name.identifier(Target::allowedTargets.name)) as? IrVararg
            ?: return null
        return valueArgument.elements.filterIsInstance<IrGetEnumValue>()
            .mapNotNull { KotlinTarget.valueOrNull(it.symbol.owner.name.asString()) }
            .toSet()
    }
}