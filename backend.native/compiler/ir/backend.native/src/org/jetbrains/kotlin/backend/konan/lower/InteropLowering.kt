/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cgen.*
import org.jetbrains.kotlin.backend.konan.descriptors.allOverriddenFunctions
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.ir.companionObject
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.backend.konan.llvm.tryGetIntrinsicType
import org.jetbrains.kotlin.backend.konan.serialization.resolveFakeOverrideMaybeAbstract
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal class InteropLowering(context: Context) : FileLoweringPass {
    // TODO: merge these lowerings.
    private val part1 = InteropLoweringPart1(context)
    private val part2 = InteropLoweringPart2(context)

    override fun lower(irFile: IrFile) {
        part1.lower(irFile)
        part2.lower(irFile)
    }
}

private abstract class BaseInteropIrTransformer(private val context: Context) : IrBuildingTransformer(context) {

    protected inline fun <T> generateWithStubs(element: IrElement? = null, block: KotlinStubs.() -> T): T =
            createKotlinStubs(element).block()

    protected fun createKotlinStubs(element: IrElement?): KotlinStubs {
        val location = if (element != null) {
            element.getCompilerMessageLocation(irFile)
        } else {
            builder.getCompilerMessageLocation()
        }

        val uniqueModuleName = irFile.packageFragmentDescriptor.module.name.asString()
                .let { it.substring(1, it.lastIndex) }
        val uniquePrefix = buildString {
            append('_')
            uniqueModuleName.toByteArray().joinTo(this, "") {
                (0xFF and it.toInt()).toString(16).padStart(2, '0')
            }
            append('_')
        }

        return object : KotlinStubs {
            override val irBuiltIns get() = context.irBuiltIns
            override val symbols get() = context.ir.symbols

            override fun addKotlin(declaration: IrDeclaration) {
                addTopLevel(declaration)
            }

            override fun addC(lines: List<String>) {
                context.cStubsManager.addStub(location, lines)
            }

            override fun getUniqueCName(prefix: String) =
                    "$uniquePrefix${context.cStubsManager.getUniqueName(prefix)}"

            override fun getUniqueKotlinFunctionReferenceClassName(prefix: String) =
                    "$prefix${context.functionReferenceCount++}"

            override val target get() = context.config.target

            override fun reportError(location: IrElement, message: String): Nothing =
                    context.reportCompilationError(message, irFile, location)

            override fun throwCompilerError(element: IrElement?, message: String): Nothing {
                error(irFile, element, message)
            }
        }
    }

    protected abstract val irFile: IrFile
    protected abstract fun addTopLevel(declaration: IrDeclaration)
}

private class InteropLoweringPart1(val context: Context) : BaseInteropIrTransformer(context), FileLoweringPass {

    private val symbols get() = context.ir.symbols

    lateinit var currentFile: IrFile

    private val topLevelInitializers = mutableListOf<IrExpression>()
    private val newTopLevelDeclarations = mutableListOf<IrDeclaration>()

    override val irFile: IrFile
        get() = currentFile

    override fun addTopLevel(declaration: IrDeclaration) {
        declaration.parent = currentFile
        newTopLevelDeclarations += declaration
    }

    override fun lower(irFile: IrFile) {
        currentFile = irFile
        irFile.transformChildrenVoid(this)

        topLevelInitializers.forEach { irFile.addTopLevelInitializer(it, context, false) }
        topLevelInitializers.clear()

        irFile.addChildren(newTopLevelDeclarations)
        newTopLevelDeclarations.clear()
    }

    private fun IrBuilderWithScope.callAlloc(classPtr: IrExpression): IrExpression =
            irCall(symbols.interopAllocObjCObject).apply {
                putValueArgument(0, classPtr)
            }

    private fun IrBuilderWithScope.getObjCClass(classSymbol: IrClassSymbol): IrExpression {
        val classDescriptor = classSymbol.descriptor
        assert(!classDescriptor.isObjCMetaClass())
        return irCall(symbols.interopGetObjCClass, symbols.nativePtrType, listOf(classSymbol.typeWithStarProjections))
    }

    private val outerClasses = mutableListOf<IrClass>()

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.isKotlinObjCClass()) {
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

        irClass.declarations.toList().mapNotNull {
            when {
                it is IrSimpleFunction && it.annotations.hasAnnotation(interop.objCAction.fqNameSafe) ->
                        generateActionImp(it)

                it is IrProperty && it.annotations.hasAnnotation(interop.objCOutlet.fqNameSafe) ->
                        generateOutletSetterImp(it)

                it is IrConstructor && it.annotations.hasAnnotation(interop.objCOverrideInit.fqNameSafe) ->
                        generateOverrideInit(irClass, it)

                else -> null
            }
        }.let { irClass.addChildren(it) }

        if (irClass.annotations.hasAnnotation(interop.exportObjCClass.fqNameSafe)) {
            val irBuilder = context.createIrBuilder(currentFile.symbol).at(irClass)
            topLevelInitializers.add(irBuilder.getObjCClass(irClass.symbol))
        }
    }

    private fun generateOverrideInit(irClass: IrClass, constructor: IrConstructor): IrSimpleFunction {
        val superClass = irClass.getSuperClassNotAny()!!
        val superConstructors = superClass.constructors.filter {
            constructor.overridesConstructor(it)
        }.toList()

        val superConstructor = superConstructors.singleOrNull() ?: run {
            val annotation = context.interopBuiltIns.objCOverrideInit.name
            if (superConstructors.isEmpty()) {
                context.reportCompilationError(
                        """
                            constructor with @$annotation doesn't override any super class constructor.
                            It must completely match by parameter names and types.""".trimIndent(),
                        currentFile,
                        constructor
                )
            } else {
                context.reportCompilationError(
                        "constructor with @$annotation matches more than one of super constructors",
                        currentFile,
                        constructor
                )
            }
        }

        val initMethod = superConstructor.getObjCInitMethod()!!

        // Remove fake overrides of this init method, also check for explicit overriding:
        irClass.declarations.removeAll {
            if (it is IrSimpleFunction && initMethod.symbol in it.overriddenSymbols) {
                if (it.isReal) {
                    val annotation = context.interopBuiltIns.objCOverrideInit.name
                    context.reportCompilationError(
                            "constructor with @$annotation overrides initializer that is already overridden explicitly",
                            currentFile,
                            constructor
                    )
                }
                true
            } else {
                false
            }
        }

        // Generate `override fun init...(...) = this.initBy(...)`:

        val resultDescriptor = WrappedSimpleFunctionDescriptor()
        return IrFunctionImpl(
                constructor.startOffset, constructor.endOffset,
                OVERRIDING_INITIALIZER_BY_CONSTRUCTOR,
                IrSimpleFunctionSymbolImpl(resultDescriptor),
                initMethod.name,
                Visibilities.PUBLIC,
                Modality.OPEN,
                irClass.defaultType,
                isInline = false,
                isExternal = false,
                isTailrec = false,
                isSuspend = false,
                isExpect = false,
                isFakeOverride = false,
                isOperator = false,
                isInfix = false
        ).also { result ->
            resultDescriptor.bind(result)
            result.parent = irClass
            result.createDispatchReceiverParameter()
            result.valueParameters += constructor.valueParameters.map { it.copyTo(result) }

            result.overriddenSymbols += initMethod.symbol

            result.body = context.createIrBuilder(result.symbol).irBlockBody(result) {
                +irReturn(
                        irCall(symbols.interopObjCObjectInitBy, listOf(irClass.defaultType)).apply {
                            extensionReceiver = irGet(result.dispatchReceiverParameter!!)
                            putValueArgument(0, irCall(constructor).also {
                                result.valueParameters.forEach { parameter ->
                                    it.putValueArgument(parameter.index, irGet(parameter))
                                }
                            })
                        }
                )
            }

            assert(result.getObjCMethodInfo() != null) // Ensure it gets correctly recognized by the compiler.
        }
    }

    private object OVERRIDING_INITIALIZER_BY_CONSTRUCTOR :
            IrDeclarationOriginImpl("OVERRIDING_INITIALIZER_BY_CONSTRUCTOR")

    private fun IrConstructor.overridesConstructor(other: IrConstructor): Boolean {
        return this.descriptor.valueParameters.size == other.descriptor.valueParameters.size &&
                this.descriptor.valueParameters.all {
                    val otherParameter = other.descriptor.valueParameters[it.index]
                    it.name == otherParameter.name && it.type == otherParameter.type
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

        val returnType = function.returnType

        if (!returnType.isUnit()) {
            context.reportCompilationError("Unexpected $action method return type: ${returnType.toKotlinType()}\n" +
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

        val name = property.name.asString()
        val selector = "set${name.capitalize()}:"

        return generateFunctionImp(selector, property.setter!!)
    }

    private fun getMethodSignatureEncoding(function: IrFunction): String {
        assert(function.extensionReceiverParameter == null)
        assert(function.valueParameters.all { it.type.isObjCObjectType() })
        assert(function.returnType.isUnit())

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

        val returnType = function.returnType
        assert(returnType.isUnit())

        val nativePtrType = context.ir.symbols.nativePtrType

        val parameterTypes = mutableListOf(nativePtrType) // id self

        parameterTypes.add(nativePtrType) // SEL _cmd

        function.valueParameters.mapTo(parameterTypes) {
            when {
                it.descriptor.type.isObjCObjectType() -> nativePtrType
                else -> TODO()
            }
        }

        val newFunction = WrappedSimpleFunctionDescriptor().let {
            IrFunctionImpl(
                    function.startOffset, function.endOffset,
                    IrDeclarationOrigin.DEFINED,
                    IrSimpleFunctionSymbolImpl(it),
                    ("imp:$selector").synthesizedName,
                    Visibilities.PRIVATE,
                    Modality.FINAL,
                    returnType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false,
                    isExpect = false,
                    isFakeOverride = false,
                    isOperator = false,
                    isInfix = false
            ).apply {
                it.bind(this)
            }
        }

        newFunction.valueParameters += parameterTypes.mapIndexed { index, type ->
            WrappedValueParameterDescriptor().let {
                IrValueParameterImpl(
                        function.startOffset, function.endOffset,
                        IrDeclarationOrigin.DEFINED,
                        IrValueParameterSymbolImpl(it),
                        Name.identifier("p$index"),
                        index,
                        type,
                        varargElementType = null,
                        isCrossinline = false,
                        isNoinline = false
                ).apply {
                    it.bind(this)
                    parent = newFunction
                }
            }
        }

        // Annotations to be detected in KotlinObjCClassInfoGenerator:

        newFunction.annotations += buildSimpleAnnotation(context.irBuiltIns, function.startOffset, function.endOffset,
                symbols.objCMethodImp.owner, selector, signatureEncoding)

        val builder = context.createIrBuilder(newFunction.symbol)
        newFunction.body = builder.irBlockBody(newFunction) {
            +irCall(function).apply {
                dispatchReceiver = interpretObjCPointer(
                        irGet(newFunction.valueParameters[0]),
                        function.dispatchReceiverParameter!!.type
                )

                function.valueParameters.forEachIndexed { index, parameter ->
                    putValueArgument(index,
                            interpretObjCPointer(
                                    irGet(newFunction.valueParameters[index + 2]),
                                    parameter.type
                            )
                    )
                }
            }
        }

        return newFunction
    }

    private fun IrBuilderWithScope.interpretObjCPointer(expression: IrExpression, type: IrType): IrExpression {
        val callee: IrFunctionSymbol = if (type.containsNull()) {
            symbols.interopInterpretObjCPointerOrNull
        } else {
            symbols.interopInterpretObjCPointer
        }

        return irCall(callee, listOf(type)).apply {
            putValueArgument(0, expression)
        }
    }

    private fun IrClass.hasFields() =
            this.declarations.any {
                when (it) {
                    is IrField ->  it.isReal
                    is IrProperty -> it.isReal && it.backingField != null
                    else -> false
                }
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

        irClass.companionObject()?.let {
            if (it.hasFields() ||
                    it.getSuperClassNotAny()?.hasFields() ?: false) {
                context.reportCompilationError(
                        "Fields are not supported for Companion of subclass of ObjC type",
                        currentFile, irClass
                )
            }
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
            val overriddenMethodOfAny = method.allOverriddenFunctions.firstOrNull {
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

        if (!constructedClass.isObjCClass()) {
            return expression
        }

        constructedClass.parent.let { parent ->
            if (parent is IrClass && parent.isObjCClass() &&
                    constructedClass.isCompanion) {

                // Note: it is actually not used; getting values of such objects is handled by code generator
                // in [FunctionGenerationContext.getObjectValue].

                return expression
            }
        }

        val delegatingCallConstructingClass = expression.symbol.owner.constructedClass
        if (!constructedClass.isExternalObjCClass() &&
            delegatingCallConstructingClass.isExternalObjCClass()) {

            // Calling super constructor from Kotlin Objective-C class.

            assert(constructedClass.getSuperClassNotAny() == delegatingCallConstructingClass)

            val initMethod = expression.symbol.owner.getObjCInitMethod()!!

            if (!expression.symbol.owner.objCConstructorIsDesignated()) {
                context.reportCompilationError(
                        "Unable to call non-designated initializer as super constructor",
                        currentFile,
                        expression
                )
            }

            val initMethodInfo = initMethod.getExternalObjCMethodInfo()!!

            assert(expression.dispatchReceiver == null)
            assert(expression.extensionReceiver == null)

            val initCall = builder.genLoweredObjCMethodCall(
                    initMethodInfo,
                    superQualifier = delegatingCallConstructingClass.symbol,
                    receiver = builder.irGet(constructedClass.thisReceiver!!),
                    arguments = initMethod.valueParameters.map { expression.getValueArgument(it.index) },
                    call = expression,
                    method = initMethod
            )

            val superConstructor = delegatingCallConstructingClass
                    .constructors.single { it.valueParameters.size == 0 }.symbol

            return builder.irBlock(expression) {
                // Required for the IR to be valid, will be ignored in codegen:
                +IrDelegatingConstructorCallImpl(
                        startOffset,
                        endOffset,
                        context.irBuiltIns.unitType,
                        superConstructor,
                        0
                )

                +irCall(symbols.interopObjCObjectSuperInitCheck).apply {
                    extensionReceiver = irGet(constructedClass.thisReceiver!!)
                    putValueArgument(0, initCall)
                }
            }
        }

        return expression
    }

    private fun IrBuilderWithScope.genLoweredObjCMethodCall(
            info: ObjCMethodInfo,
            superQualifier: IrClassSymbol?,
            receiver: IrExpression,
            arguments: List<IrExpression?>,
            call: IrFunctionAccessExpression,
            method: IrSimpleFunction
    ): IrExpression = genLoweredObjCMethodCall(
            info = info,
            superQualifier = superQualifier,
            receiver = ObjCCallReceiver.Regular(rawPtr = getRawPtr(receiver)),
            arguments = arguments,
            call = call,
            method = method
    )

    private fun IrBuilderWithScope.genLoweredObjCMethodCall(
            info: ObjCMethodInfo,
            superQualifier: IrClassSymbol?,
            receiver: ObjCCallReceiver,
            arguments: List<IrExpression?>,
            call: IrFunctionAccessExpression,
            method: IrSimpleFunction
    ): IrExpression = generateWithStubs(call) {
        if (method.parent !is IrClass) {
            // Category-provided.
            this@InteropLoweringPart1.context.llvmImports.add(method.llvmSymbolOrigin)
        }

        this.generateObjCCall(
                this@genLoweredObjCMethodCall,
                method,
                info.isStret,
                info.selector,
                call,
                superQualifier,
                receiver,
                arguments
        )
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        expression.transformChildrenVoid()

        val callee = expression.symbol.owner
        val initMethod = callee.getObjCInitMethod()
        if (initMethod != null) {
            val arguments = callee.valueParameters.map { expression.getValueArgument(it.index) }
            assert(expression.extensionReceiver == null)
            assert(expression.dispatchReceiver == null)

            val constructedClass = callee.constructedClass
            val initMethodInfo = initMethod.getExternalObjCMethodInfo()!!
            return builder.at(expression).run {
                val classPtr = getObjCClass(constructedClass.symbol)
                ensureObjCReferenceNotNull(callAllocAndInit(classPtr, initMethodInfo, arguments, expression, initMethod))
            }
        }

        return expression
    }

    private fun IrBuilderWithScope.ensureObjCReferenceNotNull(expression: IrExpression): IrExpression =
            if (!expression.type.containsNull()) {
                expression
            } else {
                irBlock(resultType = expression.type) {
                    val temp = irTemporary(expression)
                    +irIfThen(
                            context.irBuiltIns.unitType,
                            irEqeqeq(irGet(temp), irNull()),
                            irCall(symbols.ThrowNullPointerException)
                    )
                    +irGet(temp)
                }
            }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        val callee = expression.symbol.owner

        callee.getObjCFactoryInitMethodInfo()?.let { initMethodInfo ->
            val arguments = (0 until expression.valueArgumentsCount)
                    .map { index -> expression.getValueArgument(index) }

            return builder.at(expression).run {
                val classPtr = getRawPtr(expression.extensionReceiver!!)
                callAllocAndInit(classPtr, initMethodInfo, arguments, expression, callee as IrSimpleFunction)
            }
        }

        callee.getExternalObjCMethodInfo()?.let { methodInfo ->
            val isInteropStubsFile =
                    currentFile.annotations.hasAnnotation(FqName("kotlinx.cinterop.InteropStubs"))

            // Special case: bridge from Objective-C method implementation template to Kotlin method;
            // handled in CodeGeneratorVisitor.callVirtual.
            val useKotlinDispatch = isInteropStubsFile &&
                    builder.scope.scopeOwner.annotations.hasAnnotation(FqName("kotlin.native.internal.ExportForCppRuntime"))

            if (!useKotlinDispatch) {
                val arguments = callee.valueParameters.map { expression.getValueArgument(it.index) }
                assert(expression.dispatchReceiver == null || expression.extensionReceiver == null)

                if (expression.superQualifierSymbol?.owner?.isObjCMetaClass() == true) {
                    context.reportCompilationError(
                            "Super calls to Objective-C meta classes are not supported yet",
                            currentFile, expression
                    )
                }

                if (expression.superQualifierSymbol?.owner?.isInterface == true) {
                    context.reportCompilationError(
                            "Super calls to Objective-C protocols are not allowed",
                            currentFile, expression
                    )
                }

                builder.at(expression)
                return builder.genLoweredObjCMethodCall(
                        methodInfo,
                        superQualifier = expression.superQualifierSymbol,
                        receiver = expression.dispatchReceiver ?: expression.extensionReceiver!!,
                        arguments = arguments,
                        call = expression,
                        method = callee as IrSimpleFunction
                )
            }
        }

        return when (callee.symbol) {
            symbols.interopTypeOf -> {
                val typeArgument = expression.getSingleTypeArgument()
                val classSymbol = typeArgument.classifierOrNull as? IrClassSymbol

                if (classSymbol == null) {
                    expression
                } else {
                    val irClass = classSymbol.owner

                    val companionObject = irClass.companionObject() ?:
                            error("native variable class ${irClass.descriptor} must have the companion object")

                    builder.at(expression).irGetObject(companionObject.symbol)
                }
            }
            else -> expression
        }
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val backingField = declaration.backingField
        return if (declaration.isConst && backingField?.isStatic == true && context.config.isInteropStubs) {
            // Transform top-level `const val x = 42` to `val x get() = 42`.
            // Generally this transformation is just an optimization to ensure that interop constants
            // don't require any storage and/or initialization at program startup.
            // Also it is useful due to uncertain design of top-level stored properties in Kotlin/Native.
            val initializer = backingField.initializer!!.expression
            declaration.backingField = null

            val getter = declaration.getter!!
            val getterBody = getter.body!! as IrBlockBody
            getterBody.statements.clear()
            getterBody.statements += IrReturnImpl(
                    declaration.startOffset,
                    declaration.endOffset,
                    context.irBuiltIns.nothingType,
                    getter.symbol,
                    initializer
            )
            // Note: in interop stubs const val initializer is either `IrConst` or quite simple expression,
            // so it is ok to compute it every time.

            assert(declaration.setter == null)
            assert(!declaration.isVar)

            declaration.transformChildrenVoid()
            declaration
        } else {
            super.visitProperty(declaration)
        }
    }

    private fun IrBuilderWithScope.callAllocAndInit(
            classPtr: IrExpression,
            initMethodInfo: ObjCMethodInfo,
            arguments: List<IrExpression?>,
            call: IrFunctionAccessExpression,
            initMethod: IrSimpleFunction
    ): IrExpression = genLoweredObjCMethodCall(
            initMethodInfo,
            superQualifier = null,
            receiver = ObjCCallReceiver.Retained(rawPtr = callAlloc(classPtr)),
            arguments = arguments,
            call = call,
            method = initMethod
    )

    private fun IrBuilderWithScope.getRawPtr(receiver: IrExpression) =
            irCall(symbols.interopObjCObjectRawValueGetter).apply {
                extensionReceiver = receiver
            }
}

/**
 * Lowers some interop intrinsic calls.
 */
private class InteropLoweringPart2(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = InteropTransformer(context, irFile)
        irFile.transformChildrenVoid(transformer)

        while (transformer.newTopLevelDeclarations.isNotEmpty()) {
            val newTopLevelDeclarations = transformer.newTopLevelDeclarations.toList()
            transformer.newTopLevelDeclarations.clear()

            // Assuming these declarations contain only new IR (i.e. existing lowered IR has not been moved there).
            // TODO: make this more reliable.
            val loweredNewTopLevelDeclarations =
                    newTopLevelDeclarations.map { it.transform(transformer, null) as IrDeclaration }

            irFile.addChildren(loweredNewTopLevelDeclarations)
        }
    }
}

private class InteropTransformer(val context: Context, override val irFile: IrFile) : BaseInteropIrTransformer(context) {

    val newTopLevelDeclarations = mutableListOf<IrDeclaration>()

    val interop = context.interopBuiltIns
    val symbols = context.ir.symbols

    override fun addTopLevel(declaration: IrDeclaration) {
        declaration.parent = irFile
        newTopLevelDeclarations += declaration
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        super.visitClass(declaration)
        if (declaration.isKotlinObjCClass()) {
            val uniq = mutableSetOf<String>()  // remove duplicates [KT-38234]
            val imps = declaration.simpleFunctions().filter { it.isReal }.flatMap { function ->
                function.overriddenSymbols.mapNotNull {
                    val selector = it.owner.getExternalObjCMethodInfo()?.selector
                    if (selector == null || selector in uniq) {
                        null
                    } else {
                        uniq += selector
                        generateWithStubs(it.owner) {
                            generateCFunctionAndFakeKotlinExternalFunction(
                                    function,
                                    it.owner,
                                    isObjCMethod = true,
                                    location = function
                            )
                        }
                    }
                }
            }
            declaration.addChildren(imps)
        }
        return declaration
    }

    private fun generateCFunctionPointer(function: IrSimpleFunction, expression: IrExpression): IrExpression =
            generateWithStubs { generateCFunctionPointer(function, function, expression) }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        expression.transformChildrenVoid(this)

        val callee = expression.symbol.owner
        val inlinedClass = callee.returnType.getInlinedClassNative()
        if (inlinedClass?.descriptor == interop.cPointer || inlinedClass?.descriptor == interop.nativePointed) {
            context.reportCompilationError("Native interop types constructors must not be called directly",
                irFile, expression)
        }

        val constructedClass = callee.constructedClass
        if (!constructedClass.isObjCClass())
            return expression

        assert(constructedClass.isKotlinObjCClass()) // Calls to other ObjC class constructors must be lowered.
        return builder.at(expression).irBlock {
            val rawPtr = irTemporary(irCall(symbols.interopAllocObjCObject.owner).apply {
                putValueArgument(0, getObjCClass(symbols, constructedClass.symbol))
            })
            val instance = irTemporary(irCall(symbols.interopInterpretObjCPointer.owner).apply {
                putValueArgument(0, irGet(rawPtr))
            })
            // Balance pointer retained by alloc:
            +irCall(symbols.interopObjCRelease.owner).apply {
                putValueArgument(0, irGet(rawPtr))
            }
            +irCall(symbols.initInstance).apply {
                putValueArgument(0, irGet(instance))
                putValueArgument(1, expression)
            }
            +irGet(instance)
        }
    }

    /**
     * Handle `const val`s that come from interop libraries.
     *
     * We extract constant value from the backing field, and replace getter invocation with it.
     */
    private fun tryGenerateInteropConstantRead(expression: IrCall): IrExpression? {
        val function = expression.symbol.owner

        if (!function.descriptor.module.isFromInteropLibrary()) return null
        if (!function.isGetter) return null

        val constantProperty = (function as? IrSimpleFunction)
                ?.correspondingPropertySymbol
                ?.owner
                ?.takeIf { it.isConst }
                ?: return null

        val irConstant = (constantProperty.backingField
                ?.initializer
                ?.expression
                ?: error("Constant property ${constantProperty.name} has no initializer!"))
                as IrConst<*>

        // Avoid node duplication
        return irConstant.copy()
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val intrinsicType = tryGetIntrinsicType(expression)
        if (intrinsicType == IntrinsicType.OBJC_INIT_BY) {
            // Need to do this separately as otherwise [expression.transformChildrenVoid(this)] would be called
            // and the [IrConstructorCall] would be transformed which is not what we want.
            val intrinsic = interop.objCObjectInitBy.name

            val argument = expression.getValueArgument(0)!!
            val constructorCall = argument as? IrConstructorCall
                    ?: context.reportCompilationError("Argument of '$intrinsic' must be a constructor call",
                            irFile, argument)

            val constructedClass = constructorCall.symbol.owner.constructedClass

            val extensionReceiver = expression.extensionReceiver!!
            if (extensionReceiver !is IrGetValue ||
                    !extensionReceiver.symbol.owner.isDispatchReceiverFor(constructedClass)) {

                context.reportCompilationError("Receiver of '$intrinsic' must be a 'this' of the constructed class",
                        irFile, extensionReceiver)
            }

            constructorCall.transformChildrenVoid(this)

            return builder.at(expression).irBlock {
                val instance = extensionReceiver.symbol.owner
                +irCall(symbols.initInstance).apply {
                    putValueArgument(0, irGet(instance))
                    putValueArgument(1, constructorCall)
                }
                +irGet(instance)
            }
        }

        expression.transformChildrenVoid(this)
        builder.at(expression)
        val function = expression.symbol.owner

        if ((function as? IrSimpleFunction)?.resolveFakeOverrideMaybeAbstract()?.symbol
                == symbols.interopNativePointedRawPtrGetter) {

            // Replace by the intrinsic call to be handled by code generator:
            return builder.irCall(symbols.interopNativePointedGetRawPointer).apply {
                extensionReceiver = expression.dispatchReceiver
            }
        }

        if (function.annotations.hasAnnotation(RuntimeNames.cCall)) {
            context.llvmImports.add(function.llvmSymbolOrigin)
            return generateWithStubs { generateCCall(expression, builder, isInvoke = false) }
        }

        val failCompilation = { msg: String -> context.reportCompilationError(msg, irFile, expression) }
        tryGenerateInteropMemberAccess(expression, symbols, builder, failCompilation)?.let { return it }

        tryGenerateInteropConstantRead(expression)?.let { return it }

        if (intrinsicType != null) {
            return when (intrinsicType) {
                IntrinsicType.INTEROP_BITS_TO_FLOAT -> {
                    val argument = expression.getValueArgument(0)
                    if (argument is IrConst<*> && argument.kind == IrConstKind.Int) {
                        val floatValue = kotlinx.cinterop.bitsToFloat(argument.value as Int)
                        builder.irFloat(floatValue)
                    } else {
                        expression
                    }
                }
                IntrinsicType.INTEROP_BITS_TO_DOUBLE -> {
                    val argument = expression.getValueArgument(0)
                    if (argument is IrConst<*> && argument.kind == IrConstKind.Long) {
                        val doubleValue = kotlinx.cinterop.bitsToDouble(argument.value as Long)
                        builder.irDouble(doubleValue)
                    } else {
                        expression
                    }
                }
                IntrinsicType.INTEROP_STATIC_C_FUNCTION -> {
                    val irCallableReference = unwrapStaticFunctionArgument(expression.getValueArgument(0)!!)

                    if (irCallableReference == null || irCallableReference.getArguments().isNotEmpty()
                            || irCallableReference.symbol !is IrSimpleFunctionSymbol) {
                        context.reportCompilationError(
                                "${function.fqNameForIrSerialization} must take an unbound, non-capturing function or lambda",
                                irFile, expression
                        )
                        // TODO: should probably be reported during analysis.
                    }

                    val targetSymbol = irCallableReference.symbol
                    val target = targetSymbol.owner
                    val signatureTypes = target.allParameters.map { it.type } + target.returnType

                    function.typeParameters.indices.forEach { index ->
                        val typeArgument = expression.getTypeArgument(index)!!.toKotlinType()
                        val signatureType = signatureTypes[index].toKotlinType()
                        if (typeArgument.constructor != signatureType.constructor ||
                                typeArgument.isMarkedNullable != signatureType.isMarkedNullable) {
                            context.reportCompilationError(
                                    "C function signature element mismatch: expected '$signatureType', got '$typeArgument'",
                                    irFile, expression
                            )
                        }
                    }

                    generateCFunctionPointer(target as IrSimpleFunction, expression)
                }
                IntrinsicType.INTEROP_FUNPTR_INVOKE -> {
                    generateWithStubs { generateCCall(expression, builder, isInvoke = true) }
                }
                IntrinsicType.INTEROP_SIGN_EXTEND, IntrinsicType.INTEROP_NARROW -> {

                    val integerTypePredicates = arrayOf(
                            IrType::isByte, IrType::isShort, IrType::isInt, IrType::isLong
                    )

                    val receiver = expression.extensionReceiver!!
                    val typeOperand = expression.getSingleTypeArgument()
                    val kotlinTypeOperand = typeOperand.toKotlinType()

                    val receiverTypeIndex = integerTypePredicates.indexOfFirst { it(receiver.type) }
                    val typeOperandIndex = integerTypePredicates.indexOfFirst { it(typeOperand) }

                    val receiverKotlinType = receiver.type.toKotlinType()

                    if (receiverTypeIndex == -1) {
                        context.reportCompilationError("Receiver's type $receiverKotlinType is not an integer type",
                                irFile, receiver)
                    }

                    if (typeOperandIndex == -1) {
                        context.reportCompilationError("Type argument $kotlinTypeOperand is not an integer type",
                                irFile, expression)
                    }

                    when (intrinsicType) {
                        IntrinsicType.INTEROP_SIGN_EXTEND -> if (receiverTypeIndex > typeOperandIndex) {
                            context.reportCompilationError("unable to sign extend $receiverKotlinType to $kotlinTypeOperand",
                                    irFile, expression)
                        }

                        IntrinsicType.INTEROP_NARROW -> if (receiverTypeIndex < typeOperandIndex) {
                            context.reportCompilationError("unable to narrow $receiverKotlinType to $kotlinTypeOperand",
                                    irFile, expression)
                        }

                        else -> context.reportCompilationError("unexpected intrinsic $intrinsicType",
                                irFile, expression)
                    }

                    val receiverClass = symbols.integerClasses.single {
                        receiver.type.isSubtypeOf(it.owner.defaultType)
                    }
                    val targetClass = symbols.integerClasses.single {
                        typeOperand.isSubtypeOf(it.owner.defaultType)
                    }

                    val conversionSymbol = receiverClass.functions.single {
                        it.owner.name == Name.identifier("to${targetClass.owner.name}")
                    }

                    builder.irCall(conversionSymbol).apply {
                        dispatchReceiver = receiver
                    }
                }
                IntrinsicType.INTEROP_CONVERT -> {
                    val integerClasses = symbols.allIntegerClasses
                    val typeOperand = expression.getTypeArgument(0)!!
                    val receiverType = expression.symbol.owner.extensionReceiverParameter!!.type
                    val source = receiverType.classifierOrFail as IrClassSymbol
                    assert(source in integerClasses)

                    if (typeOperand is IrSimpleType && typeOperand.classifier in integerClasses && !typeOperand.hasQuestionMark) {
                        val target = typeOperand.classifier as IrClassSymbol
                        val valueToConvert = expression.extensionReceiver!!

                        if (source in symbols.signedIntegerClasses && target in symbols.unsignedIntegerClasses) {
                            // Default Kotlin signed-to-unsigned widening integer conversions don't follow C rules.
                            val signedTarget = symbols.unsignedToSignedOfSameBitWidth[target]!!
                            val widened = builder.irConvertInteger(source, signedTarget, valueToConvert)
                            builder.irConvertInteger(signedTarget, target, widened)
                        } else {
                            builder.irConvertInteger(source, target, valueToConvert)
                        }
                    } else {
                        context.reportCompilationError(
                                "unable to convert ${receiverType.toKotlinType()} to ${typeOperand.toKotlinType()}",
                                irFile,
                                expression
                        )
                    }
                }
                IntrinsicType.INTEROP_MEMORY_COPY -> {
                    TODO("So far unsupported")
                }
                IntrinsicType.WORKER_EXECUTE -> {
                    val irCallableReference = unwrapStaticFunctionArgument(expression.getValueArgument(2)!!)

                    if (irCallableReference == null || irCallableReference.getArguments().isNotEmpty()) {
                        context.reportCompilationError(
                                "${function.fqNameForIrSerialization} must take an unbound, non-capturing function or lambda",
                                irFile, expression
                        )
                    }

                    val targetSymbol = irCallableReference.symbol
                    val jobPointer = IrFunctionReferenceImpl(
                            builder.startOffset, builder.endOffset,
                            symbols.executeImpl.owner.valueParameters[3].type,
                            targetSymbol,
                            typeArgumentsCount = 0,
                            reflectionTarget = null)

                    builder.irCall(symbols.executeImpl).apply {
                        putValueArgument(0, expression.dispatchReceiver)
                        putValueArgument(1, expression.getValueArgument(0))
                        putValueArgument(2, expression.getValueArgument(1))
                        putValueArgument(3, jobPointer)
                    }
                }
                else -> expression
            }
        }
        return when (function) {
            symbols.interopCPointerRawValue.owner.getter ->
                // Replace by the intrinsic call to be handled by code generator:
                builder.irCall(symbols.interopCPointerGetRawValue).apply {
                    extensionReceiver = expression.dispatchReceiver
                }
            else -> expression
        }
    }

    private fun IrBuilderWithScope.irConvertInteger(
            source: IrClassSymbol,
            target: IrClassSymbol,
            value: IrExpression
    ): IrExpression {
        val conversion = symbols.integerConversions[source to target]!!
        return irCall(conversion.owner).apply {
            if (conversion.owner.dispatchReceiverParameter != null) {
                dispatchReceiver = value
            } else {
                extensionReceiver = value
            }
        }
    }

    private fun IrType.ensureSupportedInCallbacks(isReturnType: Boolean, reportError: (String) -> Nothing) {
        this.checkCTypeNullability(reportError)

        if (isReturnType && this.isUnit()) {
            return
        }

        if (this.isPrimitiveType()) {
            return
        }

        if (UnsignedTypes.isUnsignedType(this.toKotlinType()) && !this.containsNull()) {
            return
        }

        if (this.getClass()?.descriptor == interop.cPointer) {
            return
        }

        reportError("Type ${this.toKotlinType()} is not supported in callback signature")
    }

    private fun IrType.checkCTypeNullability(reportError: (String) -> Nothing) {
        if (this.isNullablePrimitiveType() || UnsignedTypes.isUnsignedType(this.toKotlinType()) && this.containsNull()) {
            reportError("Type ${this.toKotlinType()} must not be nullable when used in C function signature")
        }

        if (this.getClass() == interop.cPointer && !this.isSimpleTypeWithQuestionMark) {
            reportError("Type ${this.toKotlinType()} must be nullable when used in C function signature")
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

    val IrValueParameter.isDispatchReceiver: Boolean
        get() = when(val parent = this.parent) {
            is IrClass -> true
            is IrFunction -> parent.dispatchReceiverParameter == this
            else -> false
        }

    private fun IrValueDeclaration.isDispatchReceiverFor(irClass: IrClass): Boolean =
        this is IrValueParameter && isDispatchReceiver && type.getClass() == irClass

}

private fun IrCall.getSingleTypeArgument(): IrType {
    val typeParameter = symbol.owner.typeParameters.single()
    return getTypeArgument(typeParameter.index)!!
}

private fun IrBuilder.irFloat(value: Float) =
        IrConstImpl.float(startOffset, endOffset, context.irBuiltIns.floatType, value)

private fun IrBuilder.irDouble(value: Double) =
        IrConstImpl.double(startOffset, endOffset, context.irBuiltIns.doubleType, value)
