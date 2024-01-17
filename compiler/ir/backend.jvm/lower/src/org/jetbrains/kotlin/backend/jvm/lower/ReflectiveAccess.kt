/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.defaultArgumentCleanerPhase
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.lower.SyntheticAccessorLowering.Companion.isAccessible
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi

// Used from CodeFragmentCompiler for IDE Debugger Plug-In
@Suppress("unused")
val reflectiveAccessLowering = makeIrFilePhase(
    ::ReflectiveAccessLowering,
    name = "ReflectiveCalls",
    description = "Avoid the need for accessors by replacing direct access to inaccessible members with accesses via reflection",
    prerequisite = setOf(defaultArgumentCleanerPhase)
)

// This lowering replaces member accesses that are illegal according to JVM
// accessibility rules with corresponding calls to the java.lang.reflect
// API. The primary use-case is to facilitate the design of the "Evaluate
// expression..." mechanism in the JVM Debugger. Here, a code fragment is
// compiled _as if_ in the context of a breakpoint. Hence, it is compiled
// against an existing class hierarchy and any access to private or otherwise
// inaccessible members that are "perceived" to be in scope must be
// transformed. The ordinary IR pipeline would introduce an accessor next to the
// access_ee_, but that is assumed to not be possible here: the accessee is
// deserialized from class files that cannot be modified at this point.
//
// The lowering looks for the following member accesses and determines their
// legality through the need for an accessor, had this been an ordinary
// compilation:
//
// - {extension, static, super*} methods, {extension} property accessors,
//     functions on companion objects
// - field accesses
// - constructor invocations
// - companion object access
//
// *super calls, private or not, are not allowed from outside the class
// hierarchy of the involved classes, so is emulated in fragment compilation by
// the use of `invokespecial` - see `invokeSpecialForCall` below.
internal class ReflectiveAccessLowering(
    val context: JvmBackendContext
) : IrElementTransformerVoidWithContext(), FileLoweringPass {

    lateinit var inlineScopeResolver: IrInlineScopeResolver

    override fun lower(irFile: IrFile) {
        inlineScopeResolver = irFile.findInlineCallSites(context)
        irFile.transformChildrenVoid(this)
    }

    // Wrapper for the logic from SyntheticAccessorLowering
    private fun IrSymbol.isAccessible(withSuper: Boolean = false): Boolean {
        return isAccessible(context, currentScope, inlineScopeResolver, withSuper, null, fromOtherClassLoader = true)
    }

    // Fragments are transformed in a post-order traversal: children first,
    // then parent. This obscures, in particular, dispatch receivers, that go
    // from `IrGetObjectValue` calls to blocks implementing the corresponding
    // reflective access. We record these _before_ transformation, in order to
    // later predict the compilation strategy for fields. See the uses of
    // `fieldLocationAndReceiver`.
    val callsOnCompanionObjects: MutableMap<IrCall, IrClassSymbol> = mutableMapOf()

    private fun recordCompanionObjectAsDispatchReceiver(expression: IrCall) {
        val dispatchReceiver = expression.dispatchReceiver as? IrGetField ?: return
        val dispatchReceiverType = dispatchReceiver.symbol.owner.type as? IrSimpleType ?: return
        val klass = dispatchReceiverType.classOrNull
        if (klass != null && klass.owner.isCompanion) {
            callsOnCompanionObjects[expression] = klass
        }
    }

    /**
     * Fragment traversal
     */

    override fun visitCall(expression: IrCall): IrExpression {
        recordCompanionObjectAsDispatchReceiver(expression)
        expression.transformChildrenVoid(this)

        val superQualifier: IrClassSymbol? = expression.superQualifierSymbol
        val callee = expression.symbol

        if (callee.isAccessible(withSuper = superQualifier != null)) {
            return expression
        }

        val isAccessToProperty = expression.symbol.owner.correspondingPropertySymbol != null
        return if (isAccessToProperty && expression.origin == IrStatementOrigin.GET_PROPERTY) {
            generateReflectiveAccessForGetter(expression)
        } else if (isAccessToProperty && expression.origin?.isAssignmentOperator() == true) {
            generateReflectiveAccessForSetter(expression)
        } else if (expression.dispatchReceiver == null && expression.extensionReceiver == null) {
            generateReflectiveStaticCall(expression)
        } else if (superQualifier != null) {
            generateInvokeSpecialForCall(expression, superQualifier)
        } else {
            generateReflectiveMethodInvocation(expression)
        }
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        expression.transformChildrenVoid(this)

        val field = expression.symbol
        return if (field.isAccessible()) {
            expression
        } else {
            generateReflectiveFieldGet(expression)
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        expression.transformChildrenVoid(this)

        val field = expression.symbol
        return if (field.isAccessible()) {
            expression
        } else if (field.owner.correspondingPropertySymbol?.owner?.isConst == true || (field.owner.isFromJava() && field.owner.isFinal)) {
            generateThrowIllegalAccessException(expression)
        } else {
            generateReflectiveFieldSet(expression)
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        expression.transformChildrenVoid(this)

        val callee = expression.symbol
        return if (callee.isAccessible()) {
            expression
        } else {
            generateReflectiveConstructorInvocation(expression)
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        expression.transformChildrenVoid(this)

        val callee = expression.symbol
        return if (callee.isAccessible()) {
            expression
        } else {
            generateReflectiveAccessForCompanion(expression)
        }
    }

    /**
     * IR Generation for java.lang.reflect.{field, method, constructor} API
     */

    private val symbols = context.ir.symbols
    private val reflectSymbols = symbols.javaLangReflectSymbols

    private fun IrBuilderWithScope.javaClassObject(klass: IrType): IrExpression =
        irCall(symbols.kClassJavaPropertyGetter).apply {
            extensionReceiver =
                IrClassReferenceImpl(
                    startOffset, endOffset,
                    context.irBuiltIns.kClassClass.starProjectedType,
                    context.irBuiltIns.kClassClass,
                    klass
                )
        }

    private fun IrBuilderWithScope.getDeclaredField(declaringClass: IrExpression, fieldName: String): IrExpression =
        irCall(reflectSymbols.getDeclaredField).apply {
            dispatchReceiver = declaringClass
            putValueArgument(0, irString(fieldName))
        }

    private fun IrBuilderWithScope.fieldSetAccessible(field: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectFieldSetAccessible).apply {
            dispatchReceiver = field
            putValueArgument(0, irTrue())
        }

    private fun IrBuilderWithScope.fieldSet(fieldObject: IrExpression, receiver: IrExpression, value: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectFieldSet).apply {
            dispatchReceiver = fieldObject
            putValueArgument(0, receiver)
            putValueArgument(1, value)
        }

    private fun IrBuilderWithScope.fieldGet(fieldObject: IrExpression, receiver: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectFieldGet).apply {
            dispatchReceiver = fieldObject
            putValueArgument(0, receiver)
        }

    private fun createBuilder(startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET) =
        context.createJvmIrBuilder(currentScope!!, startOffset, endOffset)

    private fun IrBuilderWithScope.irVararg(
        elementType: IrType,
        values: List<IrExpression>
    ): IrExpression {
        return IrArrayBuilder(createBuilder(), context.irBuiltIns.arrayClass.typeWith(elementType)).apply {
            for (value in values) {
                +value
            }
        }.build()
    }

    private fun IrBuilderWithScope.getDeclaredMethod(
        declaringClass: IrExpression,
        methodName: String,
        parameterTypes: List<IrType>
    ): IrExpression =
        irCall(reflectSymbols.getDeclaredMethod).apply {
            dispatchReceiver = declaringClass
            putValueArgument(0, irString(methodName))
            putValueArgument(1, irVararg(symbols.javaLangClass.defaultType, parameterTypes.map { javaClassObject(it) }))
        }

    private fun IrBuilderWithScope.methodSetAccessible(method: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectMethodSetAccessible).apply {
            dispatchReceiver = method
            putValueArgument(0, irTrue())
        }

    private fun IrBuilderWithScope.methodInvoke(
        method: IrExpression,
        receiver: IrExpression,
        arguments: List<IrExpression>
    ): IrExpression =
        irCall(reflectSymbols.javaLangReflectMethodInvoke).apply {
            dispatchReceiver = method
            putValueArgument(0, receiver)
            putValueArgument(1, irVararg(context.irBuiltIns.anyNType, arguments))
        }

    private fun IrBuilderWithScope.getDeclaredConstructor(
        declaringClass: IrExpression,
        parameterTypes: List<IrType>
    ): IrExpression =
        irCall(reflectSymbols.getDeclaredConstructor).apply {
            dispatchReceiver = declaringClass
            putValueArgument(0, irVararg(symbols.javaLangClass.defaultType, parameterTypes.map { javaClassObject(it) }))
        }


    private fun IrBuilderWithScope.constructorSetAccessible(constructor: IrExpression): IrExpression =
        irCall(reflectSymbols.javaLangReflectConstructorSetAccessible).apply {
            dispatchReceiver = constructor
            putValueArgument(0, irTrue())
        }

    private fun IrBuilderWithScope.constructorNewInstance(constructor: IrExpression, arguments: List<IrExpression>): IrExpression =
        irCall(reflectSymbols.javaLangReflectConstructorNewInstance).apply {
            dispatchReceiver = constructor
            putValueArgument(0, irVararg(context.irBuiltIns.anyNType, arguments))
        }

    /**
     * Specific reflective "patches"
     */

    private fun generateReflectiveMethodInvocation(
        declaringClass: IrType,
        methodName: String,
        parameterTypes: List<IrType>,
        receiver: IrExpression?, // null => static method on `declaringClass`
        arguments: List<IrExpression>,
        returnType: IrType,
        symbol: IrSymbol
    ): IrExpression =
        context.createJvmIrBuilder(symbol).irBlock(resultType = returnType) {
            val methodVar =
                createTmpVariable(
                    getDeclaredMethod(
                        javaClassObject(declaringClass),
                        methodName,
                        parameterTypes
                    ),
                    nameHint = "method",
                    irType = reflectSymbols.javaLangReflectMethod.defaultType
                )
            +methodSetAccessible(irGet(methodVar))
            +methodInvoke(irGet(methodVar), receiver ?: irNull(), arguments)
        }

    private fun IrFunctionAccessExpression.getValueArguments(): List<IrExpression> =
        (0 until valueArgumentsCount).map { getValueArgument(it)!! }

    private fun IrFunctionAccessExpression.valueParameterTypes(): List<IrType> =
        symbol.owner.valueParameters.map { it.type }

    private fun generateReflectiveMethodInvocation(call: IrCall): IrExpression {
        val parameterTypes = mutableListOf<IrType>()
        val arguments = mutableListOf<IrExpression>()

        when {
            call.extensionReceiver != null -> {
                call.symbol.owner.extensionReceiverParameter?.let { parameterTypes.add(it.type) }
                call.extensionReceiver?.let { arguments.add(it) }
            }
            call.dispatchReceiver != null && call.symbol.owner.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> {
                call.symbol.owner.dispatchReceiverParameter?.let { parameterTypes.add(it.type) }
                call.dispatchReceiver?.let { arguments.add(it) }
            }
        }

        parameterTypes.addAll(call.valueParameterTypes())
        arguments.addAll(call.getValueArguments())

        return generateReflectiveMethodInvocation(
            getDeclaredClassType(call),
            call.symbol.owner.name.asString(),
            parameterTypes,
            call.dispatchReceiver,
            arguments,
            call.type,
            call.symbol
        )
    }

    private fun generateReflectiveStaticCall(call: IrCall): IrExpression {
        assert(call.dispatchReceiver == null) { "Assumed-to-be static call with a dispatch receiver" }
        return generateReflectiveMethodInvocation(
            call.symbol.owner.parentAsClass.defaultType,
            call.symbol.owner.name.asString(),
            call.valueParameterTypes(),
            null, // static call
            call.getValueArguments(),
            call.type,
            call.symbol
        )
    }

    private fun generateReflectiveConstructorInvocation(call: IrConstructorCall): IrExpression =
        context.createJvmIrBuilder(call.symbol)
            .irBlock(resultType = call.type) {
                val constructorVar =
                    createTmpVariable(
                        getDeclaredConstructor(
                            javaClassObject(call.symbol.owner.parentAsClass.defaultType),
                            call.valueParameterTypes()
                        ),
                        nameHint = "constructor",
                        irType = reflectSymbols.javaLangReflectConstructor.defaultType
                    )
                +constructorSetAccessible(irGet(constructorVar))
                +constructorNewInstance(irGet(constructorVar), call.getValueArguments())
            }

    private fun generateReflectiveFieldGet(
        declaringClass: IrType,
        fieldName: String,
        fieldType: IrType,
        instance: IrExpression?, // null ==> static field on `declaringClass`
        symbol: IrSymbol,
    ): IrExpression =
        context.createJvmIrBuilder(symbol)
            .irBlock(resultType = fieldType) {
                val classVar = createTmpVariable(
                    javaClassObject(declaringClass),
                    nameHint = "klass",
                    irType = symbols.kClassJavaPropertyGetter.returnType
                )
                val fieldVar = createTmpVariable(
                    getDeclaredField(irGet(classVar), fieldName),
                    nameHint = "field",
                    irType = reflectSymbols.javaLangReflectField.defaultType
                )
                +fieldSetAccessible(irGet(fieldVar))
                +fieldGet(irGet(fieldVar), instance ?: irGet(classVar))
            }

    private fun generateReflectiveFieldGet(getField: IrGetField): IrExpression =
        generateReflectiveFieldGet(
            getField.symbol.owner.parentClassOrNull!!.defaultType,
            getField.symbol.owner.name.asString(),
            getField.type,
            getField.receiver,
            getField.symbol
        )

    private fun generateReflectiveFieldSet(
        declaringClass: IrType,
        fieldName: String,
        value: IrExpression,
        type: IrType,
        instance: IrExpression?,
        symbol: IrSymbol
    ): IrExpression {
        return context.createJvmIrBuilder(symbol)
            .irBlock(resultType = type) {
                val fieldVar =
                    createTmpVariable(
                        getDeclaredField(
                            javaClassObject(declaringClass),
                            fieldName
                        ),
                        nameHint = "field",
                        irType = reflectSymbols.javaLangReflectField.defaultType
                    )
                +fieldSetAccessible(irGet(fieldVar))
                +fieldSet(irGet(fieldVar), instance ?: irNull(), value)
            }
    }

    private fun generateReflectiveFieldSet(setField: IrSetField): IrExpression =
        generateReflectiveFieldSet(
            setField.symbol.owner.parentClassOrNull!!.defaultType,
            setField.symbol.owner.name.asString(),
            setField.value,
            setField.type,
            setField.receiver,
            setField.symbol,
        )

    private fun shouldUseAccessor(accessor: IrSimpleFunction): Boolean {
        return (context.generatorExtensions as StubGeneratorExtensions).isAccessorWithExplicitImplementation(accessor)
    }

    // Returns a pair of the _type_ containing the field and the _instance_ on
    // which the field should be accessed. The instance is `null` if the field
    // is static. If the field is on a companion object it will be generated on
    // the corresponding owning class (recall, at this point the field has been
    // absolutely determined to be inaccessible to outside code).
    private fun fieldLocationAndReceiver(call: IrCall): Pair<IrType, IrExpression?> {
        callsOnCompanionObjects[call]?.let {
            val parentAsClass = it.owner.parentAsClass
            if (!parentAsClass.isJvmInterface) {
                return parentAsClass.defaultType to null
            }
        }

        val type = getDeclaredClassType(call)
        return type to call.dispatchReceiver!!
    }

    private fun getDeclaredClassType(call: IrCall) =
        call.superQualifierSymbol?.defaultType ?: call.symbol.owner.resolveFakeOverrideOrFail().parentAsClass.defaultType

    private fun generateReflectiveAccessForGetter(call: IrCall): IrExpression {
        val getter = call.symbol.owner
        val property = getter.correspondingPropertySymbol!!.owner

        if (shouldUseAccessor(getter)) {
            return generateReflectiveMethodInvocation(
                getter.parentAsClass.defaultType,
                JvmAbi.getterName(propertyName = property.name.asString()),
                getter.extensionReceiverParameter?.let { listOf(it.type) } ?: listOf(),
                call.dispatchReceiver,
                listOfNotNull(call.extensionReceiver),
                getter.returnType,
                call.symbol
            )
        }

        val (fieldLocation, instance) = fieldLocationAndReceiver(call)
        return generateReflectiveFieldGet(
            fieldLocation,
            property.name.asString(),
            getter.returnType,
            instance,
            call.symbol,
        )
    }

    private fun generateReflectiveAccessForSetter(call: IrCall): IrExpression {
        val setter = call.symbol.owner
        val property = setter.correspondingPropertySymbol!!.owner

        if (shouldUseAccessor(setter)) {
            return generateReflectiveMethodInvocation(
                setter.parentAsClass.defaultType,
                JvmAbi.setterName(propertyName = property.name.asString()),
                mutableListOf<IrType>().apply {
                    setter.extensionReceiverParameter?.let { add(it.type) }
                    addAll(call.valueParameterTypes())
                },
                call.dispatchReceiver,
                mutableListOf<IrExpression>().apply {
                    call.extensionReceiver?.let { add(it) }
                    addAll(call.getValueArguments())
                },
                setter.returnType,
                call.symbol
            )
        }

        val (fieldLocation, receiver) = fieldLocationAndReceiver(call)
        return generateReflectiveFieldSet(
            fieldLocation,
            property.name.asString(),
            call.getValueArgument(0)!!,
            call.type,
            receiver,
            call.symbol
        )
    }

    private fun generateThrowIllegalAccessException(setField: IrSetField): IrExpression {
        return context.createJvmIrBuilder(setField.symbol).irBlock {
            +irCall(symbols.throwIllegalAccessException).apply {
                putValueArgument(0, irString("Can not set final field"))
            }
        }
    }


    // This is needed to coerce the codegen to emit a very specific
    // invokespecial instruction to target a super-call that is otherwise
    // illegal on the JVM. However! The byte code from this compilation is
    // not run on a JVM: it is interpreted by eval4j. Eval4j handles
    // invokespecial via JDI from which it *is* possible to do the required
    // super call.
    private fun generateInvokeSpecialForCall(expression: IrCall, superQualifier: IrClassSymbol): IrExpression {
        val jvmSignature = context.defaultMethodSignatureMapper.mapSignatureSkipGeneric(expression.symbol.owner)
        val owner = superQualifier.owner
        val builder = context.createJvmIrBuilder(expression.symbol)

        // invokeSpecial(owner: String, name: String, descriptor: String, isInterface: Boolean): T
        return builder.irCall(symbols.jvmDebuggerInvokeSpecialIntrinsic).apply {
            dispatchReceiver = expression.dispatchReceiver
            this.type = expression.symbol.owner.returnType
            putValueArgument(0, builder.irString("${owner.packageFqName}/${owner.name}"))
            putValueArgument(1, builder.irString(jvmSignature.asmMethod.name))
            putValueArgument(2, builder.irString(jvmSignature.asmMethod.descriptor))
            putValueArgument(3, builder.irFalse())
            // A workaround to pass the initial call arguments. Elements of this array
            // will be extracted and passed to the bytecode generator right before
            // generating the bytecode for invokeSpecial itself.
            val args = with(context.irBuiltIns) {
                builder.irArray(arrayClass.typeWith(anyNType)) {
                    for (i in 0 until expression.valueArgumentsCount) {
                        add(expression.getValueArgument(i)!!)
                    }
                }
            }
            putValueArgument(4, args)
        }
    }

    private fun generateReflectiveAccessForCompanion(call: IrGetObjectValue): IrExpression =
        generateReflectiveFieldGet(
            call.symbol.owner.parentAsClass.defaultType,
            "Companion",
            call.type,
            null,
            call.symbol
        )
}
