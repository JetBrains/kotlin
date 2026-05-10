/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageIssueSignificance
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.declarationsAtFunctionReferenceLowering
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.WebCallableReferenceLowering.Companion.FUNCTION_REFERENCE_IMPL
import org.jetbrains.kotlin.ir.backend.js.lower.WebCallableReferenceLowering.Companion.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.lower.getArity
import org.jetbrains.kotlin.ir.backend.js.lower.getFlags
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import kotlin.collections.plus
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSources.File as PLFile

/**
 * This lowering transforms [IrRichFunctionReference] nodes into instances of shared callable reference classes
 * optimized for WASM code size reduction.
 *
 * Unlike the generic callable reference lowering passes which create a unique class per reference, this implementation
 * deduplicates classes across multiple callable references that share the same structural characteristics:
 *   * Same super-class type (e.g., KFunctionImpl vs Any)
 *   * Same arity (number of parameters)
 *   * Same suspend status
 *   * Same KReference status (whether it's a K-prefixed reflection type)
 *   * Same bound value types (captured variables)
 *
 * While this lowering pass only coalesces per-file, the Wasm linker has support for coalescing the
 * types across files based on the key encoding these characteristics.
 *
 * Each deduplicated class contains:
 *   * A constructor that accepts:
 *     - A WASM raw function reference (wasmFuncRef) pointing to a type-erased "bridged" function
 *     - Bound values (captured variables) as fields
 *     - Platform-specific constructor parameters for reflection (name, metadata, etc.)
 *   * Fields storing the function reference and bound values
 *   * An invoke method that calls the function reference with bound and free parameters
 *
 * The bridged functions are top-level functions that:
 *   * Use type erasure (anyNType) for all non-primitive parameters and return types
 *   * Wrap the original [IrRichFunctionReference.invokeFunction] body with type casts
 *   * Enable WASM ref.func to reference them
 *   * Reference captured variables without extra boxing/unboxing.
 *
 * Class names encode their discriminating information, while and function names encode their origin path.
 *   * Classes: `{K}{Suspend}Function{N}_{boundN}`
 *   * Bridged functions: `{containerPath}_{className}${targetName}.invoke`
 *
 * Performance-wise, we don't sacrifice anything, as we added a layer of indirection for the
 * function reference pointer, but removed the extra invoke indirection since we build the casting for
 * the boxing and unboxing of typed-erased parameters directly into the bridged function itself,
 * instead of relying on the bridges lowering pass. Of course, the VM may optimize this approach differently
 * compared to the one-class-per-function-reference model.
 *
 * For example, the following code:
 * ```kotlin
 * class Foo {
 *   fun bar(x: String): Int = x.length
 *
 *   fun test() {
 *     val f1: (String) -> Int = ::bar
 *     val f2: (String) -> Int = ::bar
 *     val g1: () -> Int = this::bar.partially1("hello")
 *     val g2: () -> Int = this::bar.partially1("world")
 *   }
 * }
 * ```
 *
 * is lowered into something like (pseudocode):
 * ```kotlin
 * // Shared class for unbound method references with arity 1
 * class Function1__(
 *   func: wasmFuncRef,
 *   p$0: Any  // bound 'this' receiver
 * ) : Function1<String, Int> {
 *   private val func: wasmFuncRef = func
 *   private val f$0: Any = p$0
 *
 *   override fun invoke(p0: Any): Any =
 *     func(f$0, p0)  // Call bridged function
 * }
 *
 * // Shared class for bound method references (1 bound, arity 0)
 * class Function0_bound1(
 *   func: wasmFuncRef,
 *   p$0: Any,  // bound 'this'
 *   p$1: Any   // bound String argument
 * ) : Function0<Int> {
 *   private val func: wasmFuncRef = func
 *   private val f$0: Any = p$0
 *   private val f$1: Any = p$1
 *
 *   override fun invoke(): Any =
 *     func(f$0, f$1)
 * }
 *
 * fun Foo_test_Function1_Foo_bar$0$bar.invoke(p$0: Any, p0: Any): Any {
 *   return (p$0 as Foo).bar(p0 as String)
 * }
 *
 * fun Foo_test_Function0_Foo_bar_bound1$1$bar.invoke(p$0: Any, p$1: Any): Any {
 *   return (p$0 as Foo).bar(p$1 as String)
 * }
 *
 * // Usage site
 * fun Foo.test() {
 *   val f1 = Function1__(
 *     ref.func(Foo_test_Function1_Foo_bar$0$bar.invoke),
 *     this
 *   )
 *   val f2 = Function1__(  // Same class as f1!
 *     ref.func(Foo_test_Function1_Foo_bar$0$bar.invoke),
 *     this
 *   )
 *   val g1 = Function0_bound1(
 *     ref.func(Foo_test_Function0_Foo_bar_bound1$1$bar.invoke),
 *     this,
 *     "hello"
 *   )
 *   val g2 = Function0_bound1(  // Same class as g1!
 *     ref.func(Foo_test_Function0_Foo_bar_bound1$1$bar.invoke),
 *     this,
 *     "world"
 *   )
 * }
 * ```
 *
 * This approach significantly reduces code size compared to creating unique classes and functions for each
 * callable reference, while maintaining full functionality including reflection support.
 *
 * In addition, this lowering causes function references which do not capture any variables to be
 * initialized at load time only once as a static global variable.
 *
 */
class WasmCallableReferenceLowering(val backendContext: WasmBackendContext) : FileLoweringPass {
    private val context = backendContext

    // Per-file cache of created classes to avoid duplicates within a single file
    private val fileLocalClassCache = mutableMapOf<String, IrClass>()

    companion object {
        val STATIC_FUNCTION_REFERENCE by IrDeclarationOriginImpl.Regular
    }

    protected val IrRichFunctionReference.isKReference: Boolean
        get() = type.let { it.isKFunction() || it.isKSuspendFunction() }

    private val IrRichFunctionReference.secondFunctionInterface: IrClass?
        get() =
            // If we implement KFunctionN we also need FunctionN
            if (isKReference) {
                val referenceType = type as IrSimpleType
                val arity = referenceType.arguments.size - 1
                if (invokeFunction.isSuspend)
                    context.irBuiltIns.suspendFunctionN(arity)
                else
                    context.irBuiltIns.functionN(arity)
            } else null

    private fun getAdditionalInterfaces(reference: IrRichFunctionReference): List<IrType> =
        listOfNotNull(reference.secondFunctionInterface?.symbol?.typeWithArguments((reference.type.removeProjections() as IrSimpleType).arguments))

    private val stringType = context.irBuiltIns.stringType

    private fun postprocessInvoke(invokeFunction: IrSimpleFunction, secondFunctionInterface: IrClass?) {
        val superInvokeFun = secondFunctionInterface?.invokeFun ?: return
        invokeFunction.overriddenSymbols = invokeFunction.overriddenSymbols memoryOptimizedPlus superInvokeFun.symbol
    }

    // Pass in extra constructor arguments for reflection purposes
    private fun getExtraConstructorParameters(constructor: IrConstructor, reference: IrRichFunctionReference): List<IrValueParameter> {
        fun makeValueParameter(nameStr: String, irType: IrType): IrValueParameter =
            buildValueParameter(constructor) {
                name = Name.identifier(nameStr)
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                type = irType
                kind = IrParameterKind.Regular
            }

        val linkerError = reference.getLinkageErrorIfAny(backendContext)
        return when {
            linkerError != null -> {
                listOf(
                    makeValueParameter("message", context.irBuiltIns.stringType),
                    makeValueParameter("name", context.irBuiltIns.stringType)
                )
            }
            reference.reflectionTargetSymbol != null -> {
                listOf(
                    makeValueParameter("flags", context.irBuiltIns.intType),
                    makeValueParameter("arity", context.irBuiltIns.intType),
                    makeValueParameter("id", context.irBuiltIns.stringType),
                    makeValueParameter("receiver", context.irBuiltIns.anyNType),
                    makeValueParameter("name", context.irBuiltIns.stringType),
                )
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun IrBuilderWithScope.getExtraConstructorArgument(
        parameter: IrValueParameter,
        reference: IrRichFunctionReference,
        receiverTemp: IrVariable?,
    ): IrExpression {
        val linkerError = reference.getLinkageErrorIfAny(backendContext)
        val reflectionTargetSymbol = reference.reflectionTargetSymbol
        val name = reflectionTargetSymbol?.owner?.name?.asString() ?: ""
        return when {
            linkerError != null -> {
                when (parameter.name.asString()) {
                    "message" -> linkerError.toIrConst(context.irBuiltIns.stringType)
                    "name" -> name.toIrConst(context.irBuiltIns.stringType)
                    else -> irNull()
                }
            }
            reflectionTargetSymbol != null -> {
                when (parameter.name.asString()) {
                    "flags" -> {
                        reference.getFlags().toIrConst(context.irBuiltIns.intType)
                    }
                    "arity" -> {
                        reference.getArity().toIrConst(context.irBuiltIns.intType)
                    }
                    "id" -> {
                        reference.getId(backendContext).toIrConst(context.irBuiltIns.stringType)
                    }
                    "receiver" -> {
                        // Use the temporary variable if provided, otherwise null
                        if (receiverTemp != null) {
                            irGet(receiverTemp)
                        } else {
                            irNull()
                        }
                    }
                    "name" -> {
                        name.toIrConst(context.irBuiltIns.stringType)
                    }
                    else -> irNull()
                }
            }
            else -> irNull()
        }
    }

    private fun IrType.eraseIfReferenceType(): IrType {
        if (this.isPrimitiveType() || this.isUnsignedType()) return this
        // Nullable types are boxed in Wasm, so they must be treated as reference types to avoid
        // merging with the non-nullable variant. This applies particularly for the case of value
        // classes which get inlined later.
        if (this.isNullable()) return backendContext.irBuiltIns.anyNType
        if (this.classifierOrNull is IrTypeParameterSymbol) {
            // At the Wasm level, type parameters are passed as their upper bound, so erase them now
            // to merge properly.
            val typeParam = (this.classifierOrNull as IrTypeParameterSymbol).owner
            return (typeParam.superTypes.firstOrNull() ?: backendContext.irBuiltIns.anyNType).eraseIfReferenceType()
        }
        val clazz = this.getClass() ?: return backendContext.irBuiltIns.anyNType
        if (clazz.isSingleFieldValueClass) {
            val underlyingErased = getInlineClassUnderlyingType(clazz).eraseIfReferenceType()
            return if (underlyingErased.isPrimitiveType() || underlyingErased.isUnsignedType()) {
                this
            } else {
                backendContext.irBuiltIns.anyNType
            }
        }
        return backendContext.irBuiltIns.anyNType
    }

    override fun lower(irFile: IrFile) {
        // Clear the per-file cache for each new file
        fileLocalClassCache.clear()

        irFile.transform(object : IrTransformer<IrDeclaration?>() {
            override fun visitClass(declaration: IrClass, data: IrDeclaration?): IrStatement {
                if (declaration.isFun || declaration.symbol.isSuspendFunction() || declaration.symbol.isKSuspendFunction()) {
                    declaration.declarationsAtFunctionReferenceLowering = declaration.declarations.toList()
                }
                declaration.transformChildren(this, declaration)
                return declaration
            }

            override fun visitBody(body: IrBody, data: IrDeclaration?): IrBody {
                return data!!.factory.stageController.restrictTo(data) {
                    super.visitBody(body, data)
                }
            }

            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?): IrStatement {
                declaration.transformChildren(this, declaration)
                return declaration
            }

            override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: IrDeclaration?): IrExpression {
                expression.transformChildren(this, data)
                val irBuilder = context.createIrBuilder(
                    data!!.symbol,
                    expression.startOffset, expression.endOffset
                )

                val clazz = createOrGetFunctionReferenceClass(expression, irFile)
                val bridgedFunction = buildBridgedFunction(
                    expression,
                    irFile,
                )

                val constructorExpression = buildConstructorExpression(expression, clazz.primaryConstructor!!, irBuilder, bridgedFunction)

                // If the reference captures no variables, initialize it once in a global
                // field. However, don't do this if the reference already initializes a global
                // field, as it makes FieldInitializersLowering and DCE simpler.
                val isInGlobalFieldInitializer = data is IrField && data.isStatic
                return if (expression.boundValues.isEmpty() && !isInGlobalFieldInitializer) {
                    val fieldName =
                        Name.identifier("${clazz.name.asString()}_${expression.reflectionTargetSymbol?.owner?.name?.asString() ?: "lambda"}_singleton")
                    val singletonField = context.irFactory.buildField {
                        name = fieldName
                        type = expression.type
                        origin = STATIC_FUNCTION_REFERENCE
                        visibility = DescriptorVisibilities.PRIVATE
                        isFinal = true
                        isStatic = true
                    }.apply {
                        parent = irFile
                        initializer = context.irFactory.createExpressionBody(constructorExpression)
                    }
                    irFile.declarations += singletonField
                    irBuilder.irGetField(null, singletonField)
                } else {
                    constructorExpression
                }
            }

            override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclaration?): IrElement {
                shouldNotBeCalled()
            }
        }, null)
    }

    private fun IrType.toTypeSignatureCode(): String = when {
        this.isInt() -> "I"
        this.isLong() -> "J"
        this.isFloat() -> "F"
        this.isDouble() -> "D"
        this.isBoolean() -> "Z"
        this.isChar() -> "C"
        this.isByte() -> "B"
        this.isShort() -> "S"
        this.isUInt() -> "UI"
        this.isULong() -> "UJ"
        this.isUByte() -> "UB"
        this.isUShort() -> "US"
        this.getClass()?.isSingleFieldValueClass == true -> {
            // Only reached for value classes that ultimately wrap a primitive (reference-wrapping
            // value classes are erased to anyNType by eraseIfReferenceType, so they land in "A").
            // Use the FQN to avoid clashes between same-named classes in different packages.
            "V${this.classOrNull?.owner?.fqNameWhenAvailable?.asString()?.replace('.', '_') ?: this.classOrNull?.owner?.name?.asString() ?: "0"}"
        }
        else -> "A" // anyNType (reference type)
    }

    // This key must faithfully reflect the class type as both this lowering and the linker use this
    // string name for class deduplication.
    private fun callableReferenceKey(
        superClassType: IrType,
        arity: Int,
        isSuspend: Boolean,
        isKReference: Boolean,
        boundValueTypes: List<IrType>,
    ): String {
        val prefix = if (isKReference) "K" else ""
        val suspendPrefix = if (isSuspend) "Suspend" else ""

        val superClassSuffix = when (superClassType) {
            backendContext.wasmSymbols.reflectionSymbols.kFunctionImpl.defaultType -> "R"  // Reflection
            backendContext.wasmSymbols.reflectionSymbols.kFunctionErrorImpl.defaultType -> "E"  // Error
            else -> "" // Non-reflection (Any)
        }

        val boundInfo = if (boundValueTypes.isNotEmpty()) {
            // Encode the types of bound values to avoid collisions.
            val typeCodes = boundValueTypes.joinToString("") { it.toTypeSignatureCode() }
            "_bound${boundValueTypes.size}_$typeCodes"
        } else ""

        return "${prefix}${suspendPrefix}Function${arity}${superClassSuffix}${boundInfo}"
    }

    private fun createOrGetFunctionReferenceClass(functionReference: IrRichFunctionReference, irFile: IrFile): IrClass {
        val superClass = getSuperClassType(functionReference)
        val arity = (functionReference.type as IrSimpleType).arguments.size - 1
        val isSuspend = functionReference.invokeFunction.isSuspend
        val isKReference = functionReference.isKReference
        val boundValueTypes = functionReference.boundValues.map { it.type.eraseIfReferenceType() }

        val superInterfaceType = functionReference.type.removeProjections()
        val additionalInterfaces = getAdditionalInterfaces(functionReference)
        val key = callableReferenceKey(superClass, arity, isSuspend, isKReference, boundValueTypes)

        // Build the erased function type that matches what callRef expects.
        val anyNType = backendContext.irBuiltIns.anyNType
        val funcRefParameterTypes = boundValueTypes + List(arity) { anyNType } +
                if (isSuspend) listOf(anyNType) else emptyList()
        val erasedFunctionType = backendContext.irBuiltIns.functionN(funcRefParameterTypes.size).typeWith(funcRefParameterTypes + anyNType)

        // Check per-file cache to avoid duplicates within the same file
        fileLocalClassCache[key]?.let {
            return it
        }

        return buildFunctionReferenceClass(
            irFile, key, superClass, superInterfaceType, additionalInterfaces,
            erasedFunctionType, boundValueTypes, arity, isSuspend, functionReference
        ).also { fileLocalClassCache[key] = it }
    }

    private fun buildFunctionReferenceClass(
        irFile: IrFile,
        key: String,
        superClass: IrType,
        superInterfaceType: IrType,
        additionalInterfaces: List<IrType>,
        erasedFunctionType: IrType,
        boundValueTypes: List<IrType>,
        arity: Int,
        isSuspend: Boolean,
        functionReference: IrRichFunctionReference,
    ): IrClass {
        val functionReferenceClass = backendContext.irFactory.buildClass {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            origin = FUNCTION_REFERENCE_IMPL
            name = Name.identifier(key)
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            this.parent = irFile
            irFile.declarations += this
            createThisReceiverParameter()
        }

        functionReferenceClass.superTypes =
            listOf(superClass, superInterfaceType) memoryOptimizedPlus additionalInterfaces
        val constructor = functionReferenceClass.addConstructor {
            origin = GENERATED_MEMBER_IN_CALLABLE_REFERENCE
            isPrimary = true
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
        }.apply {
            parameters = listOf(buildValueParameter(this) {
                name = Name.identifier("func")
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                type = backendContext.wasmSymbols.wasmTypedFuncRefType(erasedFunctionType)
                kind = IrParameterKind.Regular
            }) + boundValueTypes.mapIndexed { index, type ->
                buildValueParameter(this) {
                    name = Name.identifier("p${index}")
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    this.type = type
                    kind = IrParameterKind.Regular
                }
            } + getExtraConstructorParameters(this, functionReference)
            body = backendContext.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +generateSuperClassConstructorCall(this, this@apply, superClass, functionReference)
                +IrInstanceInitializerCallImpl(
                    SYNTHETIC_OFFSET,
                    SYNTHETIC_OFFSET,
                    functionReferenceClass.symbol,
                    backendContext.irBuiltIns.unitType
                )
            }
        }

        val funcField = functionReferenceClass.addField {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("func")
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
            type = backendContext.wasmSymbols.wasmTypedFuncRefType(erasedFunctionType)
        }.apply {
            val builder = backendContext.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
            initializer = builder.irExprBody(builder.irGet(constructor.parameters[0]))
        }

        val fields = boundValueTypes.mapIndexed { index, type ->
            functionReferenceClass.addField {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                name = Name.identifier("f\$${index}")
                visibility = DescriptorVisibilities.PRIVATE
                isFinal = true
                this.type = type
            }.apply {
                val builder = backendContext.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
                initializer = builder.irExprBody(builder.irGet(constructor.parameters[index + 1]))
            }
        }

        val superFunctionSymbol = superInterfaceType.classOrFail.owner.invokeFun?.symbol
            ?: error("Super interface of callable reference must have an invoke method")

        buildInvokeMethod(
            superFunctionSymbol,
            functionReferenceClass,
            fields,
            funcField,
            arity,
            isSuspend
        ).apply {
            postprocessInvoke(this, functionReference.secondFunctionInterface)
        }

        val superInterfaceClass = superInterfaceType.classOrFail.owner

        functionReferenceClass.addFakeOverrides(
            context.typeSystem,
            buildMap {
                superInterfaceClass.declarationsAtFunctionReferenceLowering?.let { put(superInterfaceClass, it) }
            }
        )

        return functionReferenceClass
    }

    private fun buildConstructorExpression(
        reference: IrRichFunctionReference,
        constructor: IrConstructor,
        irBuilder: DeclarationIrBuilder,
        bridgedFunction: IrSimpleFunction,
    ): IrExpression {
        val boundReceiverIndex = if (reference.reflectionTargetSymbol != null) {
            val boundReceiverParameters = reference.invokeFunction.parameters.filter {
                // This is a total hack, but by the time we get to IR,
                // apparently coerced to unit function references already
                // have introduced receiver parameters (as `receiver', and
                // not as `<this>' like everywhere else. This makes things
                // fairly brittle.
                val name = it.name.asString()
                name == "<this>" || name == "receiver"
            }
            require(boundReceiverParameters.size <= 1) { "Code generation for references with more than one bound receiver is not supported yet" }
            if (boundReceiverParameters.isEmpty())
                null
            else
                reference.invokeFunction.parameters.indexOf(boundReceiverParameters.first())
        } else {
            null
        }

        fun DeclarationIrBuilder.buildConstructorCall(receiverTemp: IrVariable?) =
            irCallConstructor(constructor.symbol, emptyList()).apply {
                origin = JsStatementOrigins.CALLABLE_REFERENCE_CREATE
                arguments[0] = IrRawFunctionReferenceImpl(
                    startOffset = reference.startOffset,
                    endOffset = reference.endOffset,
                    type = reference.type,
                    symbol = bridgedFunction.symbol,
                )
                for ((index, value) in reference.boundValues.withIndex()) {
                    arguments[index + 1] = if (receiverTemp != null && index == boundReceiverIndex) {
                        irGet(receiverTemp)
                    } else {
                        value
                    }
                }
                for (index in (reference.boundValues.size + 1) until arguments.size) {
                    arguments[index] = getExtraConstructorArgument(
                        constructor.parameters[index],
                        reference,
                        receiverTemp
                    )
                }
            }

        return if (boundReceiverIndex != null) {
            // Create a temporary for the receiver to avoid duplicate IR nodes
            irBuilder.irBlock {
                val receiverTemp = irTemporary(reference.boundValues[boundReceiverIndex], nameHint = "receiver")
                +irBuilder.buildConstructorCall(receiverTemp)
            }
        } else {
            // No receiver reuse needed, use simple constructor call
            irBuilder.buildConstructorCall(null)
        }
    }

    private fun IrBuilderWithScope.irUnit() =
        IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, context.irBuiltIns.unitClass)

    private fun StringBuilder.collectNamesForLambda(d: IrDeclarationWithName) {
        val parent = d.parent

        if (parent is IrPackageFragment) {
            append(d.name.asString())
            return
        }

        collectNamesForLambda(parent as IrDeclarationWithName)

        if (d is IrAnonymousInitializer) return

        fun IrDeclaration.isLambdaFun(): Boolean = origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA

        when {
            d.isLambdaFun() -> {
                append('$')
                if (d is IrSimpleFunction && d.isSuspend) append('s')
                append("lambda")
            }
            d.name == SpecialNames.NO_NAME_PROVIDED -> append("\$o")
            else -> {
                append('$')
                append(d.name.asString())
            }
        }
    }

    private fun getBridgedFunctionName(reference: IrRichFunctionReference): Name {
        val sb = StringBuilder()
        sb.collectNamesForLambda(reference.reflectionTargetSymbol?.owner ?: reference.invokeFunction)
        sb.append(".invoke")
        return Name.identifier(sb.toString())
    }

    private fun buildBridgedFunction(
        functionReference: IrRichFunctionReference,
        irFile: IrFile,
    ): IrSimpleFunction {
        val superFunction = functionReference.overriddenFunctionSymbol.owner
        val invokeFunction = functionReference.invokeFunction

        require(superFunction.typeParameters.isEmpty()) { "Fun interface abstract function can't have type parameters" }

        val isLambda = functionReference.origin.isLambda
        val anyNType = backendContext.irBuiltIns.anyNType
        return context.irFactory.addFunction(irFile) {
            setSourceRange(if (isLambda) invokeFunction else functionReference)
            origin = IrDeclarationOrigin.DEFINED
            name = getBridgedFunctionName(functionReference)
            returnType = anyNType
            isOperator = false
            isSuspend = superFunction.isSuspend
        }.apply {
            attributeOwnerId = functionReference.attributeOwnerId
            annotations = invokeFunction.annotations

            this.parameters += invokeFunction.parameters.mapIndexed { i, parameter ->
                parameter.copyTo(
                    this,
                    type = if (i < functionReference.boundValues.size) parameter.type.eraseIfReferenceType() else anyNType
                )
            }

            val builder = context.createIrBuilder(symbol, invokeFunction.startOffset, invokeFunction.endOffset)
            body = builder.irBlockBody {
                val variablesMapping = buildMap {
                    for (i in functionReference.boundValues.size until invokeFunction.parameters.size) {
                        val invokeParameter = invokeFunction.parameters[i]
                        val erasedParameter = this@apply.parameters[i]
                        put(invokeParameter, irTemporary(irGet(erasedParameter).implicitCastTo(invokeParameter.type)))
                    }
                    for (i in functionReference.boundValues.indices) {
                        val invokeParameter = invokeFunction.parameters[i]
                        val erasedParameter = this@apply.parameters[i]
                        put(invokeParameter, irTemporary(irGet(erasedParameter).implicitCastTo(invokeParameter.type)))
                    }
                }
                val transformedBody = invokeFunction.body!!.transform(object : VariableRemapper(variablesMapping) {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        if (expression.returnTargetSymbol == invokeFunction.symbol) {
                            expression.returnTargetSymbol = this@apply.symbol
                            expression.value = expression.value.implicitCastTo(anyNType)
                        }
                        return super.visitReturn(expression)
                    }

                    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
                        if (declaration.parent == invokeFunction)
                            declaration.parent = this@apply
                        return super.visitDeclaration(declaration)
                    }
                }, null)
                if (transformedBody is IrBlockBody) {
                    +transformedBody.statements
                    if (invokeFunction.returnType.isUnit()) {
                        +irReturn(irUnit())
                    }
                } else {
                    error("Unexpected body type: ${transformedBody::class.simpleName}")
                }
            }
        }
    }

    private fun buildInvokeMethod(
        superFunctionSymbol: IrSimpleFunctionSymbol,
        functionReferenceClass: IrClass,
        boundFields: List<IrField>,
        funcField: IrField,
        arity: Int,
        isSuspend: Boolean,
    ): IrSimpleFunction {
        val anyNType = backendContext.irBuiltIns.anyNType

        return functionReferenceClass.addFunction {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            origin = GENERATED_MEMBER_IN_CALLABLE_REFERENCE
            name = Name.identifier("invoke")
            returnType = anyNType
            this.isSuspend = isSuspend
        }.apply {

            parameters += createDispatchReceiverParameterWithClassParent()

            val nonDispatchParameters = List(arity) { index ->
                buildValueParameter(this) {
                    name = Name.identifier("p${index}")
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    type = anyNType
                }
            }
            this.parameters += nonDispatchParameters
            overriddenSymbols += superFunctionSymbol
            val builder = context.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
            body = builder.irBlockBody {
                +irReturn(
                    irCall(backendContext.symbols.callRef).apply {
                        typeArguments[0] = anyNType
                        arguments[0] = irGetField(irGet(dispatchReceiverParameter!!), funcField)
                        for (boundField in boundFields) {
                            arguments.add(irGetField(irGet(dispatchReceiverParameter!!), boundField))
                        }
                        for (parameter in nonDispatchParameters) {
                            arguments.add(irGet(parameter))
                        }
                        if (isSuspend) {
                            val getContinuationSymbol = backendContext.symbols.getContinuation
                            arguments.add(
                                irCall(
                                    getContinuationSymbol,
                                    getContinuationSymbol.owner.returnType,
                                    listOf(anyNType)
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    private fun generateSuperClassConstructorCall(
        irBuilder: IrBuilderWithScope,
        constructor: IrConstructor,
        superClassType: IrType,
        functionReference: IrRichFunctionReference,
    ): IrDelegatingConstructorCall {
        return irBuilder.irDelegatingConstructorCall(superClassType.classOrFail.owner.primaryConstructor!!).apply {
            fun getConstructorArg(name: String) =
                irBuilder.irGet(constructor.parameters.find { it.name.asString() == name }
                                    ?: error("KFunctionImpl must have these constructor parameters"))

            val linkerError = functionReference.getLinkageErrorIfAny(backendContext)
            when {
                linkerError != null -> {
                    arguments[0] = getConstructorArg("message")
                    arguments[1] = getConstructorArg("name")
                }
                functionReference.reflectionTargetSymbol != null -> {
                    arguments[0] = getConstructorArg("flags")
                    arguments[1] = getConstructorArg("arity")
                    arguments[2] = getConstructorArg("id")
                    arguments[3] = getConstructorArg("receiver")
                    arguments[4] = getConstructorArg("name")
                }
            }
        }
    }

    private fun getSuperClassType(reference: IrRichFunctionReference): IrType = when {
        reference.reflectionTargetLinkageError != null -> backendContext.wasmSymbols.reflectionSymbols.kFunctionErrorImpl.defaultType
        reference.reflectionTargetSymbol != null -> backendContext.wasmSymbols.reflectionSymbols.kFunctionImpl.defaultType
        else -> backendContext.irBuiltIns.anyType
    }
}

private fun IrRichFunctionReference.getId(backendContext: WasmBackendContext): String {
    val target = if (invokeFunction.origin == IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR)
        invokeFunction.returnType.getClass()!!
    else
        reflectionTargetSymbol!!.owner
    return backendContext.idSignatureRetriever.declarationSignature(target).toString()
}

private fun IrRichFunctionReference.getLinkageErrorIfAny(backendContext: WasmBackendContext): String? =
    reflectionTargetLinkageError?.let { reflectionTargetLinkageError ->
        backendContext.partialLinkageSupport.renderAndLogLinkageError(
            reflectionTargetLinkageError,
            this,
            PLFile.determineFileFor(invokeFunction),
            PartialLinkageIssueSignificance.MINOR,
        )
    }
