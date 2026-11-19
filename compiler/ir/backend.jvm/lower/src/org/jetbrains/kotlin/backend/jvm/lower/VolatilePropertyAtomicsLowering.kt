/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendErrors
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.javaClassReference
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

/**
 * Lowers atomic intrinsics on volatile properties ("unboxed atomics"), declared in `kotlin.concurrent.atomics`
 * (see `libraries/stdlib/src/kotlin/concurrent/atomics/VolatilePropertyAtomics.common.kt`):
 *
 *     class Counter {
 *         @Volatile var value: Int = 0
 *         fun inc(): Int = this::value.incrementAndFetch()
 *     }
 *
 * Similarly to `::x.isInitialized` (see [org.jetbrains.kotlin.backend.common.lower.LateinitLowering]), the receiver of such
 * calls is a property reference literal (this is guaranteed by `FirVolatileIntrinsicApplicabilityChecker`), so the reference
 * is never materialized. Instead:
 *  * `load()`/`store()` become plain reads/writes of the (volatile) backing field;
 *  * read-modify-write operations become calls to the access mode methods of a `java.lang.invoke.VarHandle`, which is
 *    stored in a `private static final synthetic` field generated next to the backing field (one per volatile property
 *    which is used with such intrinsics). Since [java.lang.invoke.VarHandle] methods are signature polymorphic,
 *    the calls are generated with exact (unboxed) signatures, which allows the JIT to intrinsify them;
 *  * `update`, `fetchAndUpdate` and `updateAndFetch` become a CAS loop; the transform lambda is invoked directly
 *    and later inlined by [DirectInvokeLowering].
 *
 * The resulting bytecode is equivalent to the following Java code:
 *
 *     class Counter {
 *         private static final /* synthetic */ VarHandle value$VH =
 *             MethodHandles.lookup().unreflectVarHandle(Counter.class.getDeclaredField("value"));
 *         private volatile int value = 0;
 *
 *         public int inc() { return (int) value$VH.getAndAdd(this, 1) + 1; } // invokevirtual VarHandle.getAndAdd(LCounter;I)I
 *     }
 *
 * `VarHandle` is only available since JDK 9, so the intrinsics are prohibited for older JVM targets.
 *
 * The phase must run before:
 *  * [PropertyReferenceLowering], which materializes property references;
 *  * [DirectInvokeLowering], which inlines directly invoked lambdas;
 *  * [MoveOrCopyCompanionObjectFieldsLowering], since this lowering uses the final (static) fields of object properties;
 *  * `StaticInitializersLowering`, which moves the initializers of the generated fields into `<clinit>`.
 */
internal class VolatilePropertyAtomicsLowering(private val context: JvmBackendContext) : FileLoweringPass,
    IrElementTransformerVoidWithContext() {

    private val symbols = context.symbols
    private val irBuiltIns = context.irBuiltIns

    /** `VarHandle` fields to add, per host class; they are added after the file is processed to not disturb the traversal. */
    private val pendingVarHandleFields = mutableMapOf<IrClass, MutableList<IrField>>()

    private lateinit var irFile: IrFile

    override fun lower(irFile: IrFile) {
        this.irFile = irFile
        irFile.transformChildrenVoid()
        for ([host, fields] in pendingVarHandleFields) {
            // Put the fields first, so that they're initialized before any other static initializer
            // which may use the intrinsics.
            host.declarations.addAll(0, fields)
        }
        pendingVarHandleFields.clear()
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        val kind = expression.intrinsicKind() ?: return expression
        val receiverIndex = expression.symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }
        val [property, boundReceiver] = expression.arguments[receiverIndex]!!.propertyReferenceTarget()
            ?: error("Atomic intrinsic call on a non-literal property reference: ${expression.render()}")

        if (context.config.target < JvmTarget.JVM_9) {
            context.diagnosticReporter.at(expression, irFile).report(JvmBackendErrors.VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9)
            return expression
        }

        // For properties of objects and companion objects, use the static field, which will be created by
        // MoveOrCopyCompanionObjectFieldsLowering.
        val field = context.cachedDeclarations.getStaticBackingField(property)
            ?: property.backingField
            ?: error("Volatile property is supposed to have a backing field: ${property.render()}")

        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val regularArguments = expression.symbol.owner.parameters
            .filter { it.kind == IrParameterKind.Regular }
            .map { expression.arguments[it]!! }

        return builder.irBlock(resultType = expression.type) {
            // The receiver is evaluated exactly once, before the arguments (as if the property reference was materialized).
            val receiver: IrValueDeclaration? = when {
                field.isStatic -> {
                    // e.g. `SomeObject::x`, the receiver is not needed; still evaluate it for side effects.
                    if (boundReceiver != null && boundReceiver !is IrGetObjectValue && boundReceiver !is IrGetValue) +boundReceiver
                    null
                }
                else -> irTemporary(boundReceiver ?: error("Instance field without receiver: ${expression.render()}"), "receiver")
            }
            val arguments: List<() -> IrExpression> = regularArguments.map { argument ->
                if (argument is IrRichFunctionReference) {
                    // Keep lambdas in place (they're used exactly once), so that they're inlined by DirectInvokeLowering.
                    return@map { argument }
                }
                val temporary = irTemporary(argument)
                return@map { irGet(temporary) }
            }
            +lowerIntrinsic(kind, field, receiver, arguments, expression.type)
        }
    }

    private fun IrBlockBuilder.lowerIntrinsic(
        kind: IntrinsicKind,
        field: IrField,
        receiver: IrValueDeclaration?,
        arguments: List<() -> IrExpression>,
        resultType: IrType,
    ): IrExpression {
        fun arg(index: Int): IrExpression = arguments[index]()

        val valueType = field.type.erasedForVarHandle()
        val one = { if (valueType.isLong()) irLong(1) else irInt(1) }
        val minusOne = { if (valueType.isLong()) irLong(-1) else irInt(-1) }

        val result = when (kind) {
            IntrinsicKind.LOAD -> irGetField(receiver?.let { irGet(it) }, field)
            IntrinsicKind.STORE -> irSetField(receiver?.let { irGet(it) }, field, arg(0))
            IntrinsicKind.EXCHANGE -> varHandleCall(field, receiver, "getAndSet", valueType, arg(0))
            IntrinsicKind.COMPARE_AND_SET -> varHandleCall(field, receiver, "compareAndSet", irBuiltIns.booleanType, arg(0), arg(1))
            IntrinsicKind.COMPARE_AND_EXCHANGE -> varHandleCall(field, receiver, "compareAndExchange", valueType, arg(0), arg(1))
            IntrinsicKind.FETCH_AND_ADD -> varHandleCall(field, receiver, "getAndAdd", valueType, arg(0))
            IntrinsicKind.ADD_AND_FETCH -> plus(varHandleCall(field, receiver, "getAndAdd", valueType, arg(0)), arg(0))
            IntrinsicKind.FETCH_AND_INCREMENT -> varHandleCall(field, receiver, "getAndAdd", valueType, one())
            IntrinsicKind.INCREMENT_AND_FETCH -> plus(varHandleCall(field, receiver, "getAndAdd", valueType, one()), one())
            IntrinsicKind.FETCH_AND_DECREMENT -> varHandleCall(field, receiver, "getAndAdd", valueType, minusOne())
            IntrinsicKind.DECREMENT_AND_FETCH -> plus(varHandleCall(field, receiver, "getAndAdd", valueType, minusOne()), minusOne())
            IntrinsicKind.PLUS_ASSIGN -> irComposite(resultType = irBuiltIns.unitType) { +varHandleCall(field, receiver, "getAndAdd", valueType, arg(0)) }
            IntrinsicKind.MINUS_ASSIGN -> irComposite(resultType = irBuiltIns.unitType) { +varHandleCall(field, receiver, "getAndAdd", valueType, unaryMinus(arg(0))) }
            IntrinsicKind.UPDATE, IntrinsicKind.FETCH_AND_UPDATE, IntrinsicKind.UPDATE_AND_FETCH ->
                casLoop(kind, field, receiver, arg(0))
        }
        return if (result.type == resultType || resultType.isUnit()) result else irImplicitCast(result, resultType)
    }

    /**
     * ```
     * var old: T
     * var new: T
     * do {
     *     old = <field>
     *     new = transform(old)
     * } while (!VH.compareAndSet(receiver, old, new))
     * old / new / Unit
     * ```
     */
    private fun IrBlockBuilder.casLoop(
        kind: IntrinsicKind,
        field: IrField,
        receiver: IrValueDeclaration?,
        transform: IrExpression,
    ): IrExpression {
        val old = irTemporary(value = null, nameHint = "old", irType = field.type, isMutable = true)
        val new = irTemporary(value = null, nameHint = "new", irType = field.type, isMutable = true)
        val invoke = irBuiltIns.functionN(1).functions.single { it.name == OperatorNameConventions.INVOKE }
        +irDoWhile().apply {
            body = irBlock {
                +irSet(old, irGetField(receiver?.let { irGet(it) }, field))
                +irSet(new, irCall(invoke.symbol, field.type).apply {
                    dispatchReceiver = transform
                    arguments[1] = irGet(old)
                })
            }
            condition = irNot(varHandleCall(field, receiver, "compareAndSet", irBuiltIns.booleanType, irGet(old), irGet(new)))
        }
        return when (kind) {
            IntrinsicKind.UPDATE -> irUnit()
            IntrinsicKind.FETCH_AND_UPDATE -> irGet(old)
            IntrinsicKind.UPDATE_AND_FETCH -> irGet(new)
            else -> error("Unexpected intrinsic kind: $kind")
        }
    }

    /**
     * Generates `VH.<name>(receiver?, args...)` with an exact signature, as javac would do for a signature polymorphic call,
     * see [PolymorphicSignatureLowering].
     */
    private fun IrBuilderWithScope.varHandleCall(
        field: IrField,
        receiver: IrValueDeclaration?,
        name: String,
        returnType: IrType,
        vararg arguments: IrExpression,
    ): IrExpression {
        val valueType = field.type.erasedForVarHandle()
        val parameterTypes = listOfNotNull(receiver?.let { field.parentAsClass.defaultType }) + arguments.map { valueType }
        val method = polymorphicSignatureInstantiation(name, parameterTypes, returnType)
        return irCall(method.symbol, returnType).apply {
            dispatchReceiver = irGetField(null, varHandleFieldFor(field))
            var index = 1
            if (receiver != null) this.arguments[index++] = irGet(receiver)
            for (argument in arguments) this.arguments[index++] = argument
        }
    }

    private val polymorphicSignatureInstantiations = mutableMapOf<Triple<String, List<IrType>, IrType>, IrSimpleFunction>()

    private fun polymorphicSignatureInstantiation(name: String, parameterTypes: List<IrType>, returnType: IrType): IrSimpleFunction =
        polymorphicSignatureInstantiations.getOrPut(Triple(name, parameterTypes, returnType)) {
            context.irFactory.buildFun {
                this.name = Name.identifier(name)
                this.returnType = returnType
                origin = JvmLoweredDeclarationOrigin.POLYMORPHIC_SIGNATURE_INSTANTIATION
                isExternal = true
            }.apply {
                parent = symbols.varHandle.owner
                parameters += createDispatchReceiverParameterWithClassParent()
                parameterTypes.forEachIndexed { index, type -> addValueParameter("$$index", type) }
            }
        }

    /** Returns the `private static final VarHandle <field>$VH` field for the given volatile field, creating it if needed. */
    private fun varHandleFieldFor(field: IrField): IrField = field::volatileVarHandleField.getOrSetIfNull {
        val host = field.parentAsClass
        context.irFactory.buildField {
            name = Name.identifier("${field.name.asString()}\$VH")
            type = symbols.varHandle.defaultType
            origin = JvmLoweredDeclarationOrigin.VOLATILE_VAR_HANDLE_FIELD
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
            isStatic = true
        }.apply {
            parent = host
            initializer = context.createJvmIrBuilder(symbol).run {
                at(field)
                // MethodHandles.lookup().unreflectVarHandle(Host.class.getDeclaredField("field"))
                // `lookup()` is caller-sensitive: it's called from `<clinit>` of the host class, so it has private access to the field.
                irExprBody(irCall(symbols.lookupUnreflectVarHandle).apply {
                    dispatchReceiver = irCall(symbols.methodHandlesLookup)
                    arguments[1] = irCall(symbols.javaLangReflectSymbols.getDeclaredField).apply {
                        dispatchReceiver = javaClassReference(host.defaultType)
                        arguments[1] = irString(field.name.asString())
                        // The field may be renamed later by RenameFieldsLowering in case of a JVM declaration clash.
                        field::callsReferencingNameReflectively.getOrSetIfNull { mutableListOf() }.add(this)
                    }
                })
            }
            pendingVarHandleFields.getOrPut(host) { mutableListOf() }.add(this)
        }
    }

    /** `VarHandle`s are typed by the erased JVM type of the field, so type parameters are replaced with their erased upper bounds. */
    private fun IrType.erasedForVarHandle(): IrType =
        if (classifierOrNull is IrTypeParameterSymbol) erasedUpperBound.defaultType.mergeNullability(this) else this

    private fun IrBuilderWithScope.plus(left: IrExpression, right: IrExpression): IrExpression =
        irCall(left.type.binaryOperator(OperatorNameConventions.PLUS)).apply {
            dispatchReceiver = left
            arguments[1] = right
        }

    private fun IrBuilderWithScope.unaryMinus(value: IrExpression): IrExpression =
        irCall(value.type.classOrFail.owner.functions.single { it.name == OperatorNameConventions.UNARY_MINUS }.symbol).apply {
            dispatchReceiver = value
        }

    private fun IrType.binaryOperator(name: Name): IrSimpleFunctionSymbol =
        classOrFail.owner.functions.single {
            it.name == name && it.hasShape(dispatchReceiver = true, regularParameters = 1) && it.parameters[1].type == this
        }.symbol

    /** Returns the referenced property and the bound receiver, if any. */
    private fun IrExpression.propertyReferenceTarget(): Pair<IrProperty, IrExpression?>? {
        getSinglePropertyReference(this, null)?.let { reference ->
            val property = (reference.reflectionTargetSymbol as? IrPropertySymbol)?.owner?.resolveFakeOverride() ?: return null
            return property to reference.boundValues.singleOrNull()
        }
        if (this is IrPropertyReference) {
            val property = getter?.owner?.resolveFakeOverride()?.correspondingPropertySymbol?.owner ?: return null
            return property to dispatchReceiver
        }
        return null
    }

    private fun IrCall.intrinsicKind(): IntrinsicKind? {
        val callee = symbol.owner
        val kind = IntrinsicKind.byName[callee.name.asString()] ?: return null
        if (!callee.isTopLevel || callee.getPackageFragment().packageFqName != ATOMICS_PACKAGE) return null
        val receiverType = callee.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type ?: return null
        if (receiverType.classOrNull?.owner?.fqNameWhenAvailable?.toUnsafe() != StandardNames.FqNames.kMutableProperty0) return null
        return kind
    }

    private enum class IntrinsicKind(val functionName: String) {
        LOAD("load"),
        STORE("store"),
        EXCHANGE("exchange"),
        COMPARE_AND_SET("compareAndSet"),
        COMPARE_AND_EXCHANGE("compareAndExchange"),
        FETCH_AND_ADD("fetchAndAdd"),
        ADD_AND_FETCH("addAndFetch"),
        FETCH_AND_INCREMENT("fetchAndIncrement"),
        INCREMENT_AND_FETCH("incrementAndFetch"),
        FETCH_AND_DECREMENT("fetchAndDecrement"),
        DECREMENT_AND_FETCH("decrementAndFetch"),
        PLUS_ASSIGN("plusAssign"),
        MINUS_ASSIGN("minusAssign"),
        UPDATE("update"),
        FETCH_AND_UPDATE("fetchAndUpdate"),
        UPDATE_AND_FETCH("updateAndFetch");

        companion object {
            val byName: Map<String, IntrinsicKind> = entries.associateBy { it.functionName }
        }
    }

    private companion object {
        val ATOMICS_PACKAGE = FqName("kotlin.concurrent.atomics")
    }
}

private var IrField.volatileVarHandleField: IrField? by irAttribute(copyByDefault = false)

/**
 * Calls which pass the JVM name of this field as a string argument at index 1, e.g. `Class.getDeclaredField("x")` generated by
 * [VolatilePropertyAtomicsLowering]. [RenameFieldsLowering] keeps them in sync when it renames the field.
 */
internal var IrField.callsReferencingNameReflectively: MutableList<IrCall>? by irAttribute(copyByDefault = false)
