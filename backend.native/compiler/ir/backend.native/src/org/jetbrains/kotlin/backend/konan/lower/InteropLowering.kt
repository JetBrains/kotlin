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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.allOverriddenDescriptors
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isReal
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal class InteropLoweringPart1(val context: Context) : IrBuildingTransformer(context), FileLoweringPass {

    private val symbols get() = context.ir.symbols
    private val symbolTable get() = symbols.symbolTable

    lateinit var currentFile: IrFile

    private val topLevelInitializers = mutableListOf<IrExpression>()

    override fun lower(irFile: IrFile) {
        currentFile = irFile
        irFile.transformChildrenVoid(this)

        topLevelInitializers.forEach { irFile.addTopLevelInitializer(it) }
        topLevelInitializers.clear()
    }

    private fun IrBuilderWithScope.callAlloc(classSymbol: IrClassSymbol): IrExpression {
        return irCall(symbols.interopAllocObjCObject, listOf(classSymbol.descriptor.defaultType)).apply {
            putValueArgument(0, getObjCClass(classSymbol))
        }
    }

    private fun IrBuilderWithScope.getObjCClass(classSymbol: IrClassSymbol): IrExpression {
        val classDescriptor = classSymbol.descriptor
        assert(!classDescriptor.isObjCMetaClass())
        return irCall(symbols.interopGetObjCClass, listOf(classDescriptor.defaultType))
    }

    private val outerClasses = mutableListOf<IrClass>()

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.descriptor.isKotlinObjCClass()) {
            lowerKotlinObjCClass(declaration)
        }

        outerClasses.push(declaration)
        try {
            return super.visitClass(declaration)
        } finally {
            outerClasses.pop()
        }
    }

    private fun lowerKotlinObjCClass(irClass: IrClass) {
        checkKotlinObjCClass(irClass)

        val interop = context.interopBuiltIns

        irClass.declarations.mapNotNull {
            when {
                it is IrSimpleFunction && it.descriptor.annotations.hasAnnotation(interop.objCAction) ->
                        generateActionImp(it)

                it is IrProperty && it.descriptor.annotations.hasAnnotation(interop.objCOutlet) ->
                        generateOutletSetterImp(it)

                else -> null
            }
        }.let { irClass.addChildren(it) }

        if (irClass.descriptor.annotations.hasAnnotation(interop.exportObjCClass.fqNameSafe)) {
            val irBuilder = context.createIrBuilder(currentFile.symbol).at(irClass)
            topLevelInitializers.add(irBuilder.getObjCClass(irClass.symbol))
        }
    }

    private fun generateActionImp(function: IrSimpleFunction): IrSimpleFunction {
        val action = "@${context.interopBuiltIns.objCAction.name}"

        function.extensionReceiverParameter?.let {
            context.reportCompilationError("$action method must not have extension receiver",
                    currentFile, it)
        }

        function.valueParameters.forEach {
            val kotlinType = it.descriptor.type
            if (!kotlinType.isObjCObjectType()) {
                context.reportCompilationError("Unexpected $action method parameter type: $kotlinType\n" +
                        "Only Objective-C object types are supported here",
                        currentFile, it)
            }
        }

        val returnType = function.descriptor.returnType!!

        if (!returnType.isUnit()) {
            context.reportCompilationError("Unexpected $action method return type: $returnType\n" +
                    "Only 'Unit' is supported here",
                    currentFile, function
            )
        }

        return generateFunctionImp(inferObjCSelector(function.descriptor), function)
    }

    private fun generateOutletSetterImp(property: IrProperty): IrSimpleFunction {
        val descriptor = property.descriptor

        val outlet = "@${context.interopBuiltIns.objCOutlet.name}"

        if (!descriptor.isVar) {
            context.reportCompilationError("$outlet property must be var",
                    currentFile, property)
        }

        property.getter?.extensionReceiverParameter?.let {
            context.reportCompilationError("$outlet must not have extension receiver",
                    currentFile, it)
        }

        val type = descriptor.type
        if (!type.isObjCObjectType()) {
            context.reportCompilationError("Unexpected $outlet type: $type\n" +
                    "Only Objective-C object types are supported here",
                    currentFile, property)
        }

        val name = descriptor.name.asString()
        val selector = "set${name.capitalize()}:"

        return generateFunctionImp(selector, property.setter!!)
    }

    private fun getMethodSignatureEncoding(function: IrFunction): String {
        assert(function.extensionReceiverParameter == null)
        assert(function.valueParameters.all { it.type.isObjCObjectType() })
        assert(function.descriptor.returnType!!.isUnit())

        // Note: these values are valid for x86_64 and arm64.
        return when (function.valueParameters.size) {
            0 -> "v16@0:8"
            1 -> "v24@0:8@16"
            2 -> "v32@0:8@16@24"
            else -> context.reportCompilationError("Only 0, 1 or 2 parameters are supported here",
                    currentFile, function
            )
        }
    }

    private fun generateFunctionImp(selector: String, function: IrFunction): IrSimpleFunction {
        val signatureEncoding = getMethodSignatureEncoding(function)

        val returnType = function.descriptor.returnType!!
        assert(returnType.isUnit())

        val nativePtrType = context.builtIns.nativePtr.defaultType

        val parameterTypes = mutableListOf(nativePtrType) // id self

        parameterTypes.add(nativePtrType) // SEL _cmd

        function.valueParameters.mapTo(parameterTypes) {
            when {
                it.descriptor.type.isObjCObjectType() -> nativePtrType
                else -> TODO()
            }
        }

        // Annotations to be detected in KotlinObjCClassInfoGenerator:
        val annotations = createObjCMethodImpAnnotations(selector = selector, encoding = signatureEncoding)

        val newDescriptor = SimpleFunctionDescriptorImpl.create(
                function.descriptor.containingDeclaration,
                annotations,
                ("imp:" + selector).synthesizedName,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE
        )

        val valueParameters = parameterTypes.mapIndexed { index, it ->
            ValueParameterDescriptorImpl(
                    newDescriptor,
                    null,
                    index,
                    Annotations.EMPTY,
                    Name.identifier("p$index"),
                    it,
                    false,
                    false,
                    false,
                    null,
                    SourceElement.NO_SOURCE
            )
        }

        newDescriptor.initialize(
                null, null,
                emptyList(),
                valueParameters,
                returnType,
                Modality.FINAL,
                Visibilities.PRIVATE
        )

        val newFunction = IrFunctionImpl(
                function.startOffset, function.endOffset,
                IrDeclarationOrigin.DEFINED,
                newDescriptor
        ).apply { createParameterDeclarations() }

        val builder = context.createIrBuilder(newFunction.symbol)
        newFunction.body = builder.irBlockBody(newFunction) {
            +irCall(function.symbol).apply {
                dispatchReceiver = interpretObjCPointer(
                        irGet(newFunction.valueParameters[0].symbol),
                        function.dispatchReceiverParameter!!.type
                )

                function.valueParameters.forEachIndexed { index, parameter ->
                    putValueArgument(index,
                            interpretObjCPointer(
                                    irGet(newFunction.valueParameters[index + 2].symbol),
                                    parameter.type
                            )
                    )
                }
            }
        }

        return newFunction
    }

    private fun IrBuilderWithScope.interpretObjCPointer(expression: IrExpression, type: KotlinType): IrExpression {
        val callee: IrFunctionSymbol = if (TypeUtils.isNullableType(type)) {
            symbols.interopInterpretObjCPointerOrNull
        } else {
            symbols.interopInterpretObjCPointer
        }

        return irCall(callee, listOf(type)).apply {
            putValueArgument(0, expression)
        }
    }

    private fun createObjCMethodImpAnnotations(selector: String, encoding: String): Annotations {
        val annotation = AnnotationDescriptorImpl(
                context.interopBuiltIns.objCMethodImp.defaultType,
                mapOf("selector" to selector, "encoding" to encoding)
                        .mapKeys { Name.identifier(it.key) }
                        .mapValues { StringValue(it.value) },
                SourceElement.NO_SOURCE
        )

        return AnnotationsImpl(listOf(annotation))
    }

    private fun checkKotlinObjCClass(irClass: IrClass) {
        val kind = irClass.descriptor.kind
        if (kind != ClassKind.CLASS && kind != ClassKind.OBJECT) {
            context.reportCompilationError(
                    "Only classes are supported as subtypes of Objective-C types",
                    currentFile, irClass
            )
        }

        if (!irClass.descriptor.isFinalClass) {
            context.reportCompilationError(
                    "Non-final Kotlin subclasses of Objective-C classes are not yet supported",
                    currentFile, irClass
            )
        }

        var hasObjCClassSupertype = false
        irClass.descriptor.defaultType.constructor.supertypes.forEach {
            val descriptor = it.constructor.declarationDescriptor as ClassDescriptor
            if (!descriptor.isObjCClass()) {
                context.reportCompilationError(
                        "Mixing Kotlin and Objective-C supertypes is not supported",
                        currentFile, irClass
                )
            }

            if (descriptor.kind == ClassKind.CLASS) {
                hasObjCClassSupertype = true
            }
        }

        if (!hasObjCClassSupertype) {
            context.reportCompilationError(
                    "Kotlin implementation of Objective-C protocol must have Objective-C superclass (e.g. NSObject)",
                    currentFile, irClass
            )
        }

        val methodsOfAny =
                context.ir.symbols.any.owner.declarations.filterIsInstance<IrSimpleFunction>().toSet()

        irClass.declarations.filterIsInstance<IrSimpleFunction>().filter { it.isReal }.forEach { method ->
            val overriddenMethodOfAny = method.allOverriddenDescriptors.firstOrNull {
                it in methodsOfAny
            }

            if (overriddenMethodOfAny != null) {
                val correspondingObjCMethod = when (method.name.asString()) {
                    "toString" -> "'description'"
                    "hashCode" -> "'hash'"
                    "equals" -> "'isEqual:'"
                    else -> "corresponding Objective-C method"
                }

                context.report(
                        method,
                        "can't override '${method.name}', override $correspondingObjCMethod instead",
                        isError = true
                )
            }
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        expression.transformChildrenVoid()

        builder.at(expression)

        val constructedClass = outerClasses.peek()!!
        val constructedClassDescriptor = constructedClass.descriptor

        if (!constructedClassDescriptor.isObjCClass()) {
            return expression
        }

        constructedClassDescriptor.containingDeclaration.let { classContainer ->
            if (classContainer is ClassDescriptor && classContainer.isObjCClass() &&
                    constructedClassDescriptor == classContainer.companionObjectDescriptor) {

                // Note: it is actually not used; getting values of such objects is handled by code generator
                // in [FunctionGenerationContext.getObjectValue].

                return expression
            }
        }

        if (!constructedClassDescriptor.isExternalObjCClass() &&
                expression.descriptor.constructedClass.isExternalObjCClass()) {

            // Calling super constructor from Kotlin Objective-C class.

            assert(constructedClassDescriptor.getSuperClassNotAny() == expression.descriptor.constructedClass)

            val initMethod = expression.descriptor.getObjCInitMethod()!!
            val initMethodInfo = initMethod.getExternalObjCMethodInfo()!!

            assert(expression.dispatchReceiver == null)
            assert(expression.extensionReceiver == null)

            val initCall = builder.genLoweredObjCMethodCall(
                    initMethodInfo,
                    superQualifier = symbolTable.referenceClass(expression.descriptor.constructedClass),
                    receiver = builder.getRawPtr(builder.irGet(constructedClass.thisReceiver!!.symbol)),
                    arguments = initMethod.valueParameters.map { expression.getValueArgument(it)!! }
            )

            val superConstructor = symbolTable.referenceConstructor(
                    expression.descriptor.constructedClass.constructors.single { it.valueParameters.size == 0 }
            )

            return builder.irBlock(expression) {
                // Required for the IR to be valid, will be ignored in codegen:
                +IrDelegatingConstructorCallImpl(startOffset, endOffset, superConstructor, superConstructor.descriptor)

                +irCall(symbols.interopObjCObjectSuperInitCheck).apply {
                    extensionReceiver = irGet(constructedClass.thisReceiver!!.symbol)
                    putValueArgument(0, initCall)
                }
            }
        }

        return expression
    }

    private fun IrBuilderWithScope.genLoweredObjCMethodCall(info: ObjCMethodInfo, superQualifier: IrClassSymbol?,
                                         receiver: IrExpression, arguments: List<IrExpression>): IrExpression {

        val superClass = superQualifier?.let { getObjCClass(it) } ?:
                irCall(symbols.getNativeNullPtr)

        val bridge = symbolTable.referenceSimpleFunction(info.bridge)
        return irCall(bridge).apply {
            putValueArgument(0, superClass)
            putValueArgument(1, receiver)

            assert(arguments.size + 2 == info.bridge.valueParameters.size)
            arguments.forEachIndexed { index, argument ->
                putValueArgument(index + 2, argument)
            }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        val descriptor = expression.descriptor.original

        if (descriptor is ConstructorDescriptor) {
            val initMethod = descriptor.getObjCInitMethod()

            if (initMethod != null) {
                val arguments = descriptor.valueParameters.map { expression.getValueArgument(it)!! }
                assert(expression.extensionReceiver == null)
                assert(expression.dispatchReceiver == null)

                return builder.irBlock(expression) {
                    val allocated = irTemporaryVar(callAlloc(symbolTable.referenceClass(descriptor.constructedClass)))
                    val result = irTemporaryVar(
                        genLoweredObjCMethodCall(
                                initMethod.getExternalObjCMethodInfo()!!,
                                superQualifier = null,
                                receiver = irGet(allocated.symbol),
                                arguments = arguments
                        )
                    )
                    +irCall(symbols.interopObjCRelease).apply {
                        putValueArgument(0, irGet(allocated.symbol)) // Balance pointer retained by alloc.
                    }
                    +irGet(result.symbol)
                }
            }
        }

        descriptor.getExternalObjCMethodInfo()?.let { methodInfo ->
            val isInteropStubsFile =
                    currentFile.fileAnnotations.any { it.fqName ==  FqName("kotlinx.cinterop.InteropStubs") }

            // Special case: bridge from Objective-C method implementation template to Kotlin method;
            // handled in CodeGeneratorVisitor.callVirtual.
            val useKotlinDispatch = isInteropStubsFile &&
                    builder.scope.scopeOwner.annotations.hasAnnotation(FqName("konan.internal.ExportForCppRuntime"))

            if (!useKotlinDispatch) {
                val arguments = descriptor.valueParameters.map { expression.getValueArgument(it)!! }
                assert(expression.dispatchReceiver == null || expression.extensionReceiver == null)

                if (expression.superQualifier?.isObjCMetaClass() == true) {
                    context.reportCompilationError(
                            "Super calls to Objective-C meta classes are not supported yet",
                            currentFile, expression
                    )
                }

                if (expression.superQualifier?.isInterface == true) {
                    context.reportCompilationError(
                            "Super calls to Objective-C protocols are not allowed",
                            currentFile, expression
                    )
                }

                builder.at(expression)
                return builder.genLoweredObjCMethodCall(
                        methodInfo,
                        superQualifier = expression.superQualifierSymbol,
                        receiver = builder.getRawPtr(expression.dispatchReceiver ?: expression.extensionReceiver!!),
                        arguments = arguments
                )
            }
        }

        return when (descriptor) {
            context.interopBuiltIns.typeOf -> {
                val typeArgument = expression.getSingleTypeArgument()
                val classDescriptor = TypeUtils.getClassDescriptor(typeArgument)

                if (classDescriptor == null) {
                    expression
                } else {
                    val companionObjectDescriptor = classDescriptor.companionObjectDescriptor ?:
                            error("native variable class $classDescriptor must have the companion object")

                    IrGetObjectValueImpl(
                            expression.startOffset, expression.endOffset, companionObjectDescriptor.defaultType,
                            symbolTable.referenceClass(companionObjectDescriptor)
                    )
                }
            }
            else -> expression
        }
    }

    private fun IrBuilderWithScope.getRawPtr(receiver: IrExpression) =
            irCall(symbols.interopObjCObjectRawValueGetter).apply {
                extensionReceiver = receiver
            }
}

/**
 * Lowers some interop intrinsic calls.
 */
internal class InteropLoweringPart2(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = InteropTransformer(context, irFile)
        irFile.transformChildrenVoid(transformer)
    }
}

private class InteropTransformer(val context: Context, val irFile: IrFile) : IrBuildingTransformer(context) {

    val interop = context.interopBuiltIns
    val symbols = context.ir.symbols

    override fun visitCall(expression: IrCall): IrExpression {

        expression.transformChildrenVoid(this)
        builder.at(expression)
        val descriptor = expression.descriptor.original

        if (descriptor is ClassConstructorDescriptor) {
            val type = descriptor.constructedClass.defaultType
            if (type.isRepresentedAs(ValueType.C_POINTER) || type.isRepresentedAs(ValueType.NATIVE_POINTED)) {
                throw Error("Native interop types constructors must not be called directly")
            }
        }

        if (descriptor == interop.nativePointedRawPtrGetter ||
                OverridingUtil.overrides(descriptor, interop.nativePointedRawPtrGetter)) {

            // Replace by the intrinsic call to be handled by code generator:
            return builder.irCall(symbols.interopNativePointedGetRawPointer).apply {
                extensionReceiver = expression.dispatchReceiver
            }
        }

        fun reportError(message: String): Nothing = context.reportCompilationError(message, irFile, expression)

        return when (descriptor) {
            interop.cPointerRawValue.getter ->
                // Replace by the intrinsic call to be handled by code generator:
                builder.irCall(symbols.interopCPointerGetRawValue).apply {
                    extensionReceiver = expression.dispatchReceiver
                }

            interop.bitsToFloat -> {
                val argument = expression.getValueArgument(0)
                if (argument is IrConst<*> && argument.kind == IrConstKind.Int) {
                    val floatValue = kotlinx.cinterop.bitsToFloat(argument.value as Int)
                    builder.irFloat(floatValue)
                } else {
                    expression
                }
            }

            interop.bitsToDouble -> {
                val argument = expression.getValueArgument(0)
                if (argument is IrConst<*> && argument.kind == IrConstKind.Long) {
                    val doubleValue = kotlinx.cinterop.bitsToDouble(argument.value as Long)
                    builder.irDouble(doubleValue)
                } else {
                    expression
                }
            }

            in interop.staticCFunction -> {
                val irCallableReference = unwrapStaticFunctionArgument(expression.getValueArgument(0)!!)

                if (irCallableReference == null || irCallableReference.getArguments().isNotEmpty()) {
                    context.reportCompilationError(
                            "${descriptor.fqNameSafe} must take an unbound, non-capturing function or lambda",
                            irFile, expression
                    )
                    // TODO: should probably be reported during analysis.
                }

                val targetSymbol = irCallableReference.symbol
                val target = targetSymbol.descriptor
                val signatureTypes = target.allParameters.map { it.type } + target.returnType!!

                signatureTypes.forEachIndexed { index, type ->
                    type.ensureSupportedInCallbacks(
                            isReturnType = (index == signatureTypes.lastIndex),
                            reportError = ::reportError
                    )
                }

                descriptor.typeParameters.forEachIndexed { index, typeParameterDescriptor ->
                    val typeArgument = expression.getTypeArgument(typeParameterDescriptor)!!
                    val signatureType = signatureTypes[index]
                    if (typeArgument != signatureType) {
                        context.reportCompilationError(
                                "C function signature element mismatch: expected '$signatureType', got '$typeArgument'",
                                irFile, expression
                        )
                    }
                }

                IrFunctionReferenceImpl(
                        builder.startOffset, builder.endOffset,
                        expression.type,
                        targetSymbol, target,
                        typeArguments = null)
            }

            interop.scheduleFunction -> {
                val irCallableReference = unwrapStaticFunctionArgument(expression.getValueArgument(2)!!)

                if (irCallableReference == null || irCallableReference.getArguments().isNotEmpty()) {
                    context.reportCompilationError(
                            "${descriptor.fqNameSafe} must take an unbound, non-capturing function or lambda",
                            irFile, expression
                    )
                }

                val targetSymbol = irCallableReference.symbol
                val target = targetSymbol.descriptor
                val jobPointer = IrFunctionReferenceImpl(
                        builder.startOffset, builder.endOffset,
                        interop.cPointer.defaultType,
                        targetSymbol, target,
                        typeArguments = null)

                builder.irCall(symbols.scheduleImpl).apply {
                    putValueArgument(0, expression.dispatchReceiver)
                    putValueArgument(1, expression.getValueArgument(0))
                    putValueArgument(2, expression.getValueArgument(1))
                    putValueArgument(3, jobPointer)
                }
            }

            interop.signExtend, interop.narrow -> {

                val integerTypePredicates = arrayOf(
                        KotlinBuiltIns::isByte, KotlinBuiltIns::isShort, KotlinBuiltIns::isInt, KotlinBuiltIns::isLong
                )

                val receiver = expression.extensionReceiver!!
                val typeOperand = expression.getSingleTypeArgument()

                val receiverTypeIndex = integerTypePredicates.indexOfFirst { it(receiver.type) }
                val typeOperandIndex = integerTypePredicates.indexOfFirst { it(typeOperand) }

                if (receiverTypeIndex == -1) {
                    context.reportCompilationError("Receiver's type ${receiver.type} is not an integer type",
                            irFile, receiver)
                }

                if (typeOperandIndex == -1) {
                    context.reportCompilationError("Type argument $typeOperand is not an integer type",
                            irFile, expression)
                }

                when (descriptor) {
                    interop.signExtend -> if (receiverTypeIndex > typeOperandIndex) {
                        context.reportCompilationError("unable to sign extend ${receiver.type} to $typeOperand",
                                irFile, expression)
                    }

                    interop.narrow -> if (receiverTypeIndex < typeOperandIndex) {
                        context.reportCompilationError("unable to narrow ${receiver.type} to $typeOperand",
                                irFile, expression)
                    }

                    else -> throw Error()
                }

                val receiverClass = symbols.integerClasses.single {
                    receiver.type.isSubtypeOf(it.owner.defaultType)
                }
                val conversionSymbol = receiverClass.functions.single {
                    it.descriptor.name == Name.identifier("to$typeOperand")
                }

                builder.irCall(conversionSymbol).apply {
                    dispatchReceiver = receiver
                }
            }

            in interop.cFunctionPointerInvokes -> {
                // Replace by `invokeImpl${type}Ret`:

                val returnType =
                        expression.getTypeArgument(descriptor.typeParameters.single { it.name.asString() == "R" })!!

                returnType.checkCTypeNullability(::reportError)

                val invokeImpl = symbols.interopInvokeImpls[TypeUtils.getClassDescriptor(returnType)] ?:
                        context.reportCompilationError(
                                "Invocation of C function pointer with return type '$returnType' is not supported yet",
                                irFile, expression
                        )

                builder.irCall(invokeImpl).apply {
                    putValueArgument(0, expression.extensionReceiver)

                    val varargParameter = invokeImpl.descriptor.valueParameters[1]
                    val varargArgument = IrVarargImpl(
                            startOffset, endOffset, varargParameter.type, varargParameter.varargElementType!!
                    ).apply {
                        descriptor.valueParameters.forEach {
                            this.addElement(expression.getValueArgument(it)!!)
                        }
                    }
                    putValueArgument(varargParameter, varargArgument)
                }
            }

            interop.objCObjectInitBy -> {
                val intrinsic = interop.objCObjectInitBy.name

                val argument = expression.getValueArgument(0)!!
                val constructedClass =
                        ((argument as? IrCall)?.descriptor as? ClassConstructorDescriptor)?.constructedClass

                if (constructedClass == null) {
                    context.reportCompilationError("Argument of '$intrinsic' must be a constructor call",
                            irFile, argument)
                }

                val extensionReceiver = expression.extensionReceiver!!
                if (extensionReceiver !is IrGetValue ||
                        extensionReceiver.descriptor != constructedClass.thisAsReceiverParameter) {

                    context.reportCompilationError("Receiver of '$intrinsic' must be a 'this' of the constructed class",
                            irFile, extensionReceiver)
                }

                expression
            }

            else -> expression
        }
    }

    private fun KotlinType.ensureSupportedInCallbacks(isReturnType: Boolean, reportError: (String) -> Nothing) {
        this.checkCTypeNullability(reportError)

        if (isReturnType && KotlinBuiltIns.isUnit(this)) {
            return
        }

        if (KotlinBuiltIns.isPrimitiveType(this)) {
            return
        }

        if (TypeUtils.getClassDescriptor(this) == interop.cPointer) {
            return
        }

        reportError("Type $this is not supported in callback signature")
    }

    private fun KotlinType.checkCTypeNullability(reportError: (String) -> Nothing) {
        if (KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(this) && this.isMarkedNullable) {
            reportError("Type $this must not be nullable when used in C function signature")
        }

        if (TypeUtils.getClassDescriptor(this) == interop.cPointer && !this.isMarkedNullable) {
            reportError("Type $this must be nullable when used in C function signature")
        }
    }

    private fun unwrapStaticFunctionArgument(argument: IrExpression): IrFunctionReference? {
        if (argument is IrFunctionReference) {
            return argument
        }

        // Otherwise check whether it is a lambda:

        // 1. It is a container with two statements and expected origin:

        if (argument !is IrContainerExpression || argument.statements.size != 2) {
            return null
        }
        if (argument.origin != IrStatementOrigin.LAMBDA && argument.origin != IrStatementOrigin.ANONYMOUS_FUNCTION) {
            return null
        }

        // 2. First statement is an empty container (created during local functions lowering):

        val firstStatement = argument.statements.first()

        if (firstStatement !is IrContainerExpression || firstStatement.statements.size != 0) {
            return null
        }

        // 3. Second statement is IrCallableReference:

        return argument.statements.last() as? IrFunctionReference
    }
}

private fun IrCall.getSingleTypeArgument(): KotlinType {
    val typeParameter = descriptor.original.typeParameters.single()
    return getTypeArgument(typeParameter)!!
}

private fun IrBuilder.irFloat(value: Float) =
        IrConstImpl.float(startOffset, endOffset, context.builtIns.floatType, value)

private fun IrBuilder.irDouble(value: Double) =
        IrConstImpl.double(startOffset, endOffset, context.builtIns.doubleType, value)

private fun Annotations.hasAnnotation(descriptor: ClassDescriptor) = this.hasAnnotation(descriptor.fqNameSafe)
