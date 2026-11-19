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
import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.javaClassReference
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
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
 * is never materialized. The checker also prohibits properties stored in static fields (top-level properties and properties of
 * objects), so the backing field is always an instance field. Instead:
 *  * `load()`/`store()` become plain reads/writes of the (volatile) backing field;
 *  * read-modify-write operations become calls to a JDK primitive which performs an atomic operation on the backing field.
 *    The primitive object is stored in a `private static final synthetic` field generated next to the backing field
 *    (one per volatile property which is used with such intrinsics). There are two implementations, depending on the JVM target:
 *      * [VarHandleVolatilePropertyAtomicsLowering] (JVM 9+) uses `java.lang.invoke.VarHandle`;
 *      * [FieldUpdaterVolatilePropertyAtomicsLowering] (JVM 1.8) uses `java.util.concurrent.atomic.Atomic*FieldUpdater`;
 *  * `update`, `fetchAndUpdate` and `updateAndFetch` become a CAS loop; the transform lambda is invoked directly
 *    and later inlined by [DirectInvokeLowering].
 *
 * The phases must run before:
 *  * [PropertyReferenceLowering], which materializes property references;
 *  * [DirectInvokeLowering], which inlines directly invoked lambdas;
 *  * `StaticInitializersLowering`, which moves the initializers of the generated fields into `<clinit>`.
 */
internal abstract class AbstractVolatilePropertyAtomicsLowering(protected val context: JvmBackendContext) : FileLoweringPass,
    IrElementTransformerVoidWithContext() {

    protected val symbols = context.symbols
    protected val irBuiltIns = context.irBuiltIns

    /** Whether this implementation is used for the current JVM target. Exactly one implementation is applicable. */
    protected abstract val isApplicable: Boolean

    /**
     * Returns a description of the kind of properties that the given backing field belongs to,
     * if read-modify-write operations on it are not supported by this implementation (to be used in the error message),
     * or `null` otherwise.
     */
    protected open fun unsupportedPropertyKind(field: IrField): String? = null

    /** Atomically sets the field to [newValue] and returns the old value. */
    protected abstract fun IrBlockBuilder.getAndSet(access: FieldAccess, newValue: IrExpression): IrExpression

    /** Atomically sets the field to [newValue] if its current value is [expected]; returns `true` on success. */
    protected abstract fun IrBlockBuilder.compareAndSet(access: FieldAccess, expected: IrExpression, newValue: IrExpression): IrExpression

    /** Atomically sets the field to [newValue] if its current value is [expected]; returns the witness value. */
    protected abstract fun IrBlockBuilder.compareAndExchange(
        access: FieldAccess, expected: () -> IrExpression, newValue: () -> IrExpression,
    ): IrExpression

    /** Atomically adds [delta] to the field (of type `Int` or `Long`) and returns the old value. */
    protected abstract fun IrBlockBuilder.getAndAdd(access: FieldAccess, delta: IrExpression): IrExpression

    /** Atomically adds [delta] to the field (of type `Int` or `Long`) and returns the new value. */
    protected open fun IrBlockBuilder.addAndGet(access: FieldAccess, delta: () -> IrExpression): IrExpression =
        plus(getAndAdd(access, delta()), delta())

    /** The (instance) backing field of a volatile property, and the already evaluated receiver. */
    protected class FieldAccess(val field: IrField, val receiver: IrValueDeclaration)

    /** `VarHandle`s and field updaters are typed by the erased JVM type of the field. */
    protected val FieldAccess.erasedType: IrType
        get() = field.type.let { type ->
            if (type.classifierOrNull is IrTypeParameterSymbol) type.erasedUpperBound.defaultType.mergeNullability(type) else type
        }

    /** `VarHandle` / field updater fields to add, per host class; they are added after the file is processed to not disturb the traversal. */
    private val pendingHelperFields = mutableMapOf<IrClass, MutableList<IrField>>()

    private lateinit var irFile: IrFile

    override fun lower(irFile: IrFile) {
        if (!isApplicable) return
        this.irFile = irFile
        irFile.transformChildrenVoid()
        for ([host, fields] in pendingHelperFields) {
            // Put the fields first, so that they're initialized before any other static initializer
            // which may use the intrinsics.
            host.declarations.addAll(0, fields)
        }
        pendingHelperFields.clear()
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        val kind = expression.intrinsicKind() ?: return expression
        val receiverIndex = expression.symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }
        val [property, boundReceiver] = expression.arguments[receiverIndex]!!.propertyReferenceTarget()
            ?: error("Atomic intrinsic call on a non-literal property reference: ${expression.render()}")

        val field = property.backingField
            ?: error("Volatile property is supposed to have a backing field: ${property.render()}")
        check(!field.isStatic) { "Atomic intrinsics on properties stored in static fields are prohibited by the frontend: ${expression.render()}" }
        val receiverExpression = checkNotNull(boundReceiver) { "Instance field without receiver: ${expression.render()}" }

        // load/store are plain volatile accesses, they're supported everywhere.
        if (kind != IntrinsicKind.LOAD && kind != IntrinsicKind.STORE) {
            unsupportedPropertyKind(field)?.let { propertyKind ->
                context.diagnosticReporter.at(expression, irFile).report(JvmBackendErrors.VOLATILE_INTRINSIC_CALL_REQUIRES_JVM_9, propertyKind)
                return expression
            }
        }

        val builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        val regularArguments = expression.symbol.owner.parameters
            .filter { it.kind == IrParameterKind.Regular }
            .map { expression.arguments[it]!! }

        return builder.irBlock(resultType = expression.type) {
            // The receiver is evaluated exactly once, before the arguments (as if the property reference was materialized).
            val receiver = irTemporary(receiverExpression, "receiver")
            val arguments: List<() -> IrExpression> = regularArguments.map { argument ->
                if (argument is IrRichFunctionReference) {
                    // Keep lambdas in place (they're used exactly once), so that they're inlined by DirectInvokeLowering.
                    return@map { argument }
                }
                val temporary = irTemporary(argument)
                return@map { irGet(temporary) }
            }
            +lowerIntrinsic(kind, FieldAccess(field, receiver), arguments, expression.type)
        }
    }

    private fun IrBlockBuilder.lowerIntrinsic(
        kind: IntrinsicKind,
        access: FieldAccess,
        arguments: List<() -> IrExpression>,
        resultType: IrType,
    ): IrExpression {
        fun arg(index: Int): IrExpression = arguments[index]()

        val isLong = access.field.type.isLong()
        val one = { if (isLong) irLong(1) else irInt(1) }
        val minusOne = { if (isLong) irLong(-1) else irInt(-1) }

        val result = when (kind) {
            IntrinsicKind.LOAD -> load(access)
            IntrinsicKind.STORE -> irSetField(irGet(access.receiver), access.field, arg(0))
            IntrinsicKind.EXCHANGE -> getAndSet(access, arg(0))
            IntrinsicKind.COMPARE_AND_SET -> compareAndSet(access, arg(0), arg(1))
            IntrinsicKind.COMPARE_AND_EXCHANGE -> compareAndExchange(access, arguments[0], arguments[1])
            IntrinsicKind.FETCH_AND_ADD -> getAndAdd(access, arg(0))
            IntrinsicKind.ADD_AND_FETCH -> addAndGet(access, arguments[0])
            IntrinsicKind.FETCH_AND_INCREMENT -> getAndAdd(access, one())
            IntrinsicKind.INCREMENT_AND_FETCH -> addAndGet(access, one)
            IntrinsicKind.FETCH_AND_DECREMENT -> getAndAdd(access, minusOne())
            IntrinsicKind.DECREMENT_AND_FETCH -> addAndGet(access, minusOne)
            IntrinsicKind.PLUS_ASSIGN -> irComposite(resultType = irBuiltIns.unitType) { +getAndAdd(access, arg(0)) }
            IntrinsicKind.MINUS_ASSIGN -> irComposite(resultType = irBuiltIns.unitType) { +getAndAdd(access, unaryMinus(arg(0))) }
            IntrinsicKind.UPDATE, IntrinsicKind.FETCH_AND_UPDATE, IntrinsicKind.UPDATE_AND_FETCH ->
                casLoop(kind, access, arg(0))
        }
        return if (result.type == resultType || resultType.isUnit()) result else irImplicitCast(result, resultType)
    }

    protected fun IrBuilderWithScope.load(access: FieldAccess): IrExpression =
        irGetField(irGet(access.receiver), access.field)

    /**
     * ```
     * var old: T
     * var new: T
     * do {
     *     old = <field>
     *     new = transform(old)
     * } while (!compareAndSet(old, new))
     * old / new / Unit
     * ```
     */
    private fun IrBlockBuilder.casLoop(kind: IntrinsicKind, access: FieldAccess, transform: IrExpression): IrExpression {
        val old = irTemporary(value = null, nameHint = "old", irType = access.field.type, isMutable = true)
        val new = irTemporary(value = null, nameHint = "new", irType = access.field.type, isMutable = true)
        val invoke = irBuiltIns.functionN(1).functions.single { it.name == OperatorNameConventions.INVOKE }
        val loop = irDoWhile()
        loop.body = irBlock {
            +irSet(old, load(access))
            +irSet(new, irCall(invoke.symbol, access.field.type).apply {
                dispatchReceiver = transform
                arguments[1] = irGet(old)
            })
        }
        loop.condition = irNot(compareAndSet(access, irGet(old), irGet(new)))
        +loop
        return when (kind) {
            IntrinsicKind.UPDATE -> irUnit()
            IntrinsicKind.FETCH_AND_UPDATE -> irGet(old)
            IntrinsicKind.UPDATE_AND_FETCH -> irGet(new)
            else -> error("Unexpected intrinsic kind: $kind")
        }
    }

    /**
     * Emulates `compareAndExchange` with [compareAndSet], for implementations which don't have it natively:
     * ```
     * var witness: T
     * do {
     *     witness = <field>
     * } while (witness == expected && !compareAndSet(expected, newValue))
     * witness
     * ```
     * If the loop exits because the witness differs from the expected value, the witness is the value which was observed
     * by the (volatile) read; otherwise, the CAS succeeded and the witness is equal to the expected value.
     * Values are compared like in [compareAndSet]: by value for primitives, by identity for references.
     */
    protected fun IrBlockBuilder.emulatedCompareAndExchange(
        access: FieldAccess, expected: () -> IrExpression, newValue: () -> IrExpression,
    ): IrExpression {
        val witness = irTemporary(value = null, nameHint = "witness", irType = access.field.type, isMutable = true)
        val isPrimitive = access.field.type.isPrimitiveType()
        val loop = irDoWhile()
        loop.body = irSet(witness, load(access))
        loop.condition = irIfThenElse(
            irBuiltIns.booleanType,
            if (isPrimitive) irEquals(irGet(witness), expected()) else irEqeqeq(irGet(witness), expected()),
            irNot(compareAndSet(access, expected(), newValue())),
            irFalse(),
        )
        +loop
        return irGet(witness)
    }

    /**
     * Returns the `private static final <type> <field><suffix>` field for the given volatile field, creating it if needed.
     * [buildInitializer] is invoked with a builder whose scope is the new field (so that, in particular, caller-sensitive
     * methods like `MethodHandles.lookup()` are called from `<clinit>` of the host class, which has private access to the field).
     */
    protected fun helperFieldFor(
        field: IrField,
        suffix: String,
        type: IrType,
        origin: IrDeclarationOrigin,
        buildInitializer: JvmIrBuilder.(host: IrClass) -> IrExpression,
    ): IrField = field::volatileAtomicsHelperField.getOrSetIfNull {
        val host = field.parentAsClass
        context.irFactory.buildField {
            name = Name.identifier("${field.name.asString()}$suffix")
            this.type = type
            this.origin = origin
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
            isStatic = true
        }.apply {
            parent = host
            initializer = context.createJvmIrBuilder(symbol).run {
                at(field)
                irExprBody(buildInitializer(host))
            }
            pendingHelperFields.getOrPut(host) { mutableListOf() }.add(this)
        }
    }

    /** `Host.class.getDeclaredField("<field>")` */
    protected fun JvmIrBuilder.getDeclaredField(host: IrClass, field: IrField): IrExpression =
        irCall(symbols.javaLangReflectSymbols.getDeclaredField).apply {
            dispatchReceiver = javaClassReference(host.defaultType)
            putFieldName(field, 1)
        }

    /**
     * Puts the JVM name of [field] as a string argument at [index], and makes sure it's kept in sync
     * if the field is renamed later by [RenameFieldsLowering] in case of a JVM declaration clash.
     */
    protected fun IrCall.putFieldName(field: IrField, index: Int) {
        arguments[index] = IrConstImpl.string(startOffset, endOffset, irBuiltIns.stringType, field.name.asString())
        field::callsReferencingNameReflectively.getOrSetIfNull { mutableListOf() }.add(ReflectiveFieldNameUsage(this, index))
    }

    protected fun IrBuilderWithScope.plus(left: IrExpression, right: IrExpression): IrExpression =
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

/** The `VarHandle` / field updater field generated for this volatile backing field. */
private var IrField.volatileAtomicsHelperField: IrField? by irAttribute(copyByDefault = false)

/** A call which passes the JVM name of a field as a string constant [argumentIndex], e.g. `Class.getDeclaredField("x")`. */
internal data class ReflectiveFieldNameUsage(val call: IrCall, val argumentIndex: Int)

/**
 * Calls which pass the JVM name of this field as a string argument, generated by [AbstractVolatilePropertyAtomicsLowering].
 * [RenameFieldsLowering] keeps them in sync when it renames the field.
 */
internal var IrField.callsReferencingNameReflectively: MutableList<ReflectiveFieldNameUsage>? by irAttribute(copyByDefault = false)
