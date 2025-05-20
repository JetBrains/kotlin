/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.ir.returnType
import org.jetbrains.kotlin.backend.common.lower.SYNTHETIC_CATCH_FOR_FINALLY_EXPRESSION
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmSymbols
import org.jetbrains.kotlin.backend.wasm.lower.SYNTHETIC_JS_EXCEPTION_HANDLER_TO_SUPPORT_CATCH_THROWABLE
import org.jetbrains.kotlin.backend.wasm.utils.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.lower.PrimaryConstructorLowering
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

class BodyGenerator(
    private val backendContext: WasmBackendContext,
    private val wasmFileCodegenContext: WasmFileCodegenContext,
    private val functionContext: WasmFunctionCodegenContext,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
    private val wasmModuleTypeTransformer: WasmModuleTypeTransformer,
) : IrVisitorVoid() {
    val body: WasmExpressionBuilder = functionContext.bodyGen

    // Shortcuts
    private val wasmSymbols: WasmSymbols = backendContext.wasmSymbols
    private val irBuiltIns: IrBuiltIns = backendContext.irBuiltIns

    private val unitGetInstance by lazy { backendContext.findUnitGetInstanceFunction() }
    private val unitInstanceField by lazy { backendContext.findUnitInstanceField() }

    fun WasmExpressionBuilder.buildGetUnit() {
        buildGetGlobal(
            wasmFileCodegenContext.referenceGlobalField(unitInstanceField.symbol),
            SourceLocation.NoLocation("GET_UNIT")
        )
    }

    fun getStructFieldRef(field: IrField): WasmSymbol<Int> {
        val klass = field.parentAsClass
        val metadata = wasmModuleMetadataCache.getClassMetadata(klass.symbol)
        val fieldId = metadata.fields.indexOf(field) + 3 //Implicit vtable, itable and rtti fields
        return WasmSymbol(fieldId)
    }

    // Generates code for the given IR element. Leaves something on the stack unless expression was of the type Void.
    internal fun generateExpression(expression: IrExpression) {
        expression.acceptVoid(this)

        if (expression.type.isNothing()) {
            // TODO Ideally, we should generate unreachable only for specific cases and preferable on declaration site. 
            body.buildUnreachableAfterNothingType()
        }
    }

    // Generates code for the given IR element but *never* leaves anything on the stack.
    private fun generateAsStatement(statement: IrExpression) {
        generateExpression(statement)
        if (statement.type != wasmSymbols.voidType) {
            body.buildDrop(SourceLocation.NoLocation("DROP"))
        }
    }

    private fun generateStatement(statement: IrStatement) {
        when (statement) {
            is IrExpression -> generateAsStatement(statement)
            is IrVariable -> statement.acceptVoid(this)
            else -> error("Unsupported node type: ${statement::class.simpleName}")
        }
    }

    override fun visitElement(element: IrElement) {
        error("Unexpected element of type ${element::class}")
    }

    private fun tryGenerateConstVarargArray(irVararg: IrVararg, wasmArrayType: WasmImmediate.GcType): Boolean {
        if (irVararg.elements.isEmpty()) return false

        val kind = (irVararg.elements[0] as? IrConst)?.kind ?: return false
        if (kind == IrConstKind.String || kind == IrConstKind.Null) return false
        if (irVararg.elements.any { it !is IrConst || it.kind != kind }) return false

        val elementConstValues = irVararg.elements.map { (it as IrConst).value!! }

        val resource = when (irVararg.varargElementType) {
            irBuiltIns.byteType -> elementConstValues.map { (it as Byte).toLong() } to WasmI8
            irBuiltIns.booleanType -> elementConstValues.map { if (it as Boolean) 1L else 0L } to WasmI8
            irBuiltIns.intType -> elementConstValues.map { (it as Int).toLong() } to WasmI32
            irBuiltIns.shortType -> elementConstValues.map { (it as Short).toLong() } to WasmI16
            irBuiltIns.longType -> elementConstValues.map { it as Long } to WasmI64
            else -> return false
        }

        val constantArrayId = wasmFileCodegenContext.referenceConstantArray(resource)

        irVararg.getSourceLocation().let { location ->
            body.buildConstI32(0, location)
            body.buildConstI32(irVararg.elements.size, location)
            body.buildInstr(WasmOp.ARRAY_NEW_DATA, location, wasmArrayType, WasmImmediate.DataIdx(constantArrayId))
        }
        return true
    }

    private fun tryGenerateVarargArray(irVararg: IrVararg, wasmArrayType: WasmImmediate.GcType) {
        irVararg.elements.forEach {
            check(it is IrExpression)
            generateExpression(it)
        }

        val length = WasmImmediate.ConstI32(irVararg.elements.size)
        body.buildInstr(WasmOp.ARRAY_NEW_FIXED, irVararg.getSourceLocation(), wasmArrayType, length)
    }

    override fun visitVararg(expression: IrVararg) {
        val arrayClass = expression.type.getClass()!!

        val wasmArrayType = arrayClass.constructors
            .mapNotNull { it.parameters.singleOrNull()?.type }
            .firstOrNull { it.getClass()?.getWasmArrayAnnotation() != null }
            ?.getRuntimeClass(irBuiltIns)?.symbol
            ?.let(wasmFileCodegenContext::referenceGcType)
            ?.let(WasmImmediate::GcType)

        check(wasmArrayType != null)

        val location = expression.getSourceLocation()
        generateAnyParameters(arrayClass.symbol, location)
        if (!tryGenerateConstVarargArray(expression, wasmArrayType)) tryGenerateVarargArray(expression, wasmArrayType)
        body.buildStructNew(wasmFileCodegenContext.referenceGcType(expression.type.getRuntimeClass(irBuiltIns).symbol), location)
    }

    override fun visitThrow(expression: IrThrow) {
        generateExpression(expression.value)

        if (backendContext.configuration.getBoolean(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS)) {
            body.buildUnreachable(SourceLocation.NoLocation("Unreachable is inserted instead of a `throw` instruction"))
            return
        }

        val sourceLocation = expression.getSourceLocation()

        if (!backendContext.isWasmJsTarget || !backendContext.configuration.getBoolean(WasmConfigurationKeys.WASM_USE_JS_TAG)) {
            body.buildThrow(wasmFileCodegenContext.throwableTagIndex, sourceLocation)
            return
        }

        when (expression.type) {
            wasmSymbols.jsRelatedSymbols.jsException.defaultType -> {
                generateInstanceFieldAccess(wasmSymbols.jsRelatedSymbols.jsExceptionThrownValue, expression.value.getSourceLocation())
                body.buildThrow(wasmFileCodegenContext.jsExceptionTagIndex, sourceLocation)
            }
            irBuiltIns.throwableType -> {
                val tmp = functionContext.referenceLocal(functionContext.defineTmpVariable(wasmModuleTypeTransformer.transformType(irBuiltIns.throwableType)))
                body.buildSetLocal(tmp, SourceLocation.NoLocation)
                body.buildGetLocal(tmp, SourceLocation.NoLocation)
                body.buildRefTestStatic(wasmFileCodegenContext.referenceGcType(wasmSymbols.jsRelatedSymbols.jsException), sourceLocation)
                body.buildIf("is_js_exception")
                body.buildGetLocal(tmp, SourceLocation.NoLocation)
                body.buildRefCastStatic(wasmFileCodegenContext.referenceGcType(wasmSymbols.jsRelatedSymbols.jsException), SourceLocation.NoLocation)
                generateInstanceFieldAccess(wasmSymbols.jsRelatedSymbols.jsExceptionThrownValue, expression.value.getSourceLocation())
                body.buildThrow(wasmFileCodegenContext.jsExceptionTagIndex, sourceLocation)
                body.buildElse()
                body.buildGetLocal(tmp, SourceLocation.NoLocation)
                body.buildThrow(wasmFileCodegenContext.throwableTagIndex, sourceLocation)
                body.buildEnd()
            }
            else -> body.buildThrow(wasmFileCodegenContext.throwableTagIndex, sourceLocation)
        }
    }

    override fun visitTry(aTry: IrTry) {
        assert(aTry.isCanonical(backendContext)) { "expected canonical try/catch" }

        if (backendContext.configuration.getBoolean(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS)) {
            generateExpression(aTry.tryResult)
            return
        }

        if (backendContext.configuration.getBoolean(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL)) {
            generateTryFollowingNewProposal(aTry)
        } else {
            generateTryFollowingOldProposal(aTry)
        }
    }

    /**
     * The typical Kotlin try/catch:
     *
     * ```kotlin
     * try {
     *    RESULT_EXPRESSION
     * } catch (e: ExceptionType) {
     *    CATCH_EXPRESSION
     * }
     * ```
     *
     * Is translated into:
     *
     * ```wat
     * block $catch_block (BLOCK_TYPE)
     *     block $try_block (EXCEPTION_TYPE)
     *         try_table (EXCEPTION_TYPE) catch IDX $try_block
     *             TRANSLATED_RESULT_EXPRESSION
     *             br $catch_block
     *         end
     *     end
     *     TRANSLATED_CATCH_EXPRESSION
     * end
     * ```
     *
     */
    private fun generateTryFollowingNewProposal(aTry: IrTry) {
        val canUseJsTag = backendContext.configuration.getBoolean(WasmConfigurationKeys.WASM_USE_JS_TAG)

        val lastCatchBlock = aTry.catches.last()
        val firstCatchBlock = aTry.catches.first()
        val hasOnlySingleCatchBlock = lastCatchBlock === firstCatchBlock

        val resultType = wasmModuleTypeTransformer.transformBlockResultType(aTry.type)
        val needCatchAllOnly = lastCatchBlock.origin === SYNTHETIC_CATCH_FOR_FINALLY_EXPRESSION
        val areTwoCatchWithTheSameBody =
            backendContext.isWasmJsTarget && firstCatchBlock.origin === SYNTHETIC_JS_EXCEPTION_HANDLER_TO_SUPPORT_CATCH_THROWABLE

        val topLevelCatchLabel = body.buildBlock(resultType)

        val firstCatchParameterIsJsException = backendContext.isWasmJsTarget &&
                firstCatchBlock.catchParameter.type == wasmSymbols.jsRelatedSymbols.jsException.defaultType

        val nestedCatchLabel = runIf(!needCatchAllOnly && firstCatchParameterIsJsException && (!hasOnlySingleCatchBlock || !canUseJsTag)) {
            body.buildBlock(wasmModuleTypeTransformer.transformBlockResultType(irBuiltIns.throwableType))
        }

        val tryBlockType = when {
            needCatchAllOnly -> WasmExnRefType
            firstCatchParameterIsJsException -> if (canUseJsTag) WasmExternRef else null
            else -> wasmModuleTypeTransformer.transformBlockResultType(irBuiltIns.throwableType)
        }

        val tryBlockLabel = body.buildBlock(tryBlockType)

        val catchList = when {
            needCatchAllOnly -> listOf(body.createNewCatchAllRef(tryBlockLabel))
            nestedCatchLabel != null -> listOf(
                body.createNewCatch(wasmFileCodegenContext.throwableTagIndex, nestedCatchLabel),
                if (canUseJsTag) body.createNewCatch(wasmFileCodegenContext.jsExceptionTagIndex, tryBlockLabel)
                else body.createNewCatchAll(tryBlockLabel)
            )
            else -> listOf(
                body.createNewCatch(
                    if (tryBlockType === WasmExternRef) wasmFileCodegenContext.jsExceptionTagIndex else wasmFileCodegenContext.throwableTagIndex,
                    tryBlockLabel
                )
            )
        }

        body.buildTryTable(null, catchList, tryBlockType)
        generateExpression(aTry.tryResult)
        body.buildBr(topLevelCatchLabel, SourceLocation.NoLocation(""))
        body.buildEnd()

        body.buildEnd() // tryBlockLabel

        if (needCatchAllOnly) {
            val composite = lastCatchBlock.result as IrComposite
            assert(composite.statements.last().isSimpleRethrowing(lastCatchBlock)) { "Last throw is not rethrowing" }
            composite.statements.dropLast(1).forEach(::generateStatement)
            body.buildThrowRef(SourceLocation.NoLocation)
            body.buildEnd() // topLevelCatchLabel
            return
        }

        if (nestedCatchLabel != null) {
            if (!canUseJsTag) {
                body.buildRefNull(WasmHeapType.Simple.Extern, SourceLocation.NoLocation(""))
            }

            firstCatchBlock.wrapJsThrownValueIntoJsException()

            if (!areTwoCatchWithTheSameBody) {
                firstCatchBlock.initializeCatchParameter()
                generateExpression(firstCatchBlock.result)
                body.buildBr(topLevelCatchLabel, SourceLocation.NoLocation(""))
            }

            body.buildEnd() // nestedCatchLabel

            if (hasOnlySingleCatchBlock && !canUseJsTag) {
                body.buildThrow(wasmFileCodegenContext.throwableTagIndex, SourceLocation.NoLocation(""))
                body.buildEnd() // topLevelCatchLabel
                return
            }
        } else if (tryBlockType === WasmExternRef) {
            lastCatchBlock.wrapJsThrownValueIntoJsException()
        }

        lastCatchBlock.initializeCatchParameter()
        generateExpression(lastCatchBlock.result)

        body.buildEnd() // topLevelCatchLabel
    }

    private fun IrCatch.initializeCatchParameter() {
        with(catchParameter.symbol) {
            functionContext.defineLocal(this)
            body.buildSetLocal(functionContext.referenceLocal(this), owner.getSourceLocation())
        }
    }

    private fun IrCatch.wrapJsThrownValueIntoJsException() {
        body.buildCall(
            wasmFileCodegenContext.referenceFunction(wasmSymbols.jsRelatedSymbols.createJsException),
            catchParameter.getSourceLocation()
        )
    }

    private fun IrStatement.isSimpleRethrowing(catchBlock: IrCatch): Boolean =
        ((this as IrThrow).value as IrGetValue).symbol == catchBlock.catchParameter.symbol

    /**
     * The typical Kotlin try/catch:
     *
     * ```kotlin
     * try {
     *    RESULT_EXPRESSION
     * } catch (e: ExceptionType) {
     *    CATCH_EXPRESSION
     * }
     * ```
     *
     * Is translated into:
     *
     * ```wat
     * try (BLOCK_TYPE)
     *     TRANSLATED_RESULT_EXPRESSION
     * catch IDX
     *     TRANSLATED_CATCH_EXPRESSION
     * end
     * ```
     *
     */
    private fun generateTryFollowingOldProposal(aTry: IrTry) {
        val canUseJsTag = backendContext.configuration.getBoolean(WasmConfigurationKeys.WASM_USE_JS_TAG)

        val lastCatchBlock = aTry.catches.last()
        val firstCatchBlock = aTry.catches.first()
        val hasOnlySingleCatchBlock = lastCatchBlock === firstCatchBlock

        val resultType = wasmModuleTypeTransformer.transformBlockResultType(aTry.type)
        var topLevelBlockLabel: Int? = null
        val areTwoCatchWithTheSameBody = backendContext.isWasmJsTarget && (
                lastCatchBlock.origin === SYNTHETIC_CATCH_FOR_FINALLY_EXPRESSION ||
                        firstCatchBlock.origin === SYNTHETIC_JS_EXCEPTION_HANDLER_TO_SUPPORT_CATCH_THROWABLE
                )

        val lastCatchParameterIsJsException = backendContext.isWasmJsTarget &&
                lastCatchBlock.catchParameter.type == wasmSymbols.jsRelatedSymbols.jsException.defaultType

        if (areTwoCatchWithTheSameBody) {
            topLevelBlockLabel = body.buildBlock(resultType)
        }

        val tryResultType = when {
            areTwoCatchWithTheSameBody -> wasmModuleTypeTransformer.transformBlockResultType(irBuiltIns.throwableType)
            else -> resultType
        }

        body.buildTry(null, tryResultType)
        generateExpression(aTry.tryResult)

        topLevelBlockLabel?.let { body.buildBr(it, SourceLocation.NoLocation("")) }

        val tag = if (!lastCatchParameterIsJsException || !canUseJsTag)
            wasmFileCodegenContext.throwableTagIndex
        else
            wasmFileCodegenContext.jsExceptionTagIndex

        body.buildCatch(tag, SourceLocation.NextLocation)

        if (tag === wasmFileCodegenContext.jsExceptionTagIndex) {
            lastCatchBlock.wrapJsThrownValueIntoJsException()
        }

        if (lastCatchParameterIsJsException && !canUseJsTag) {
            body.buildThrow(wasmFileCodegenContext.throwableTagIndex, SourceLocation.NoLocation(""))
        } else if (!areTwoCatchWithTheSameBody) {
            lastCatchBlock.initializeCatchParameter()
            generateExpression(lastCatchBlock.result)
        }

        if (!hasOnlySingleCatchBlock || (lastCatchParameterIsJsException && !canUseJsTag) || areTwoCatchWithTheSameBody) {
            if (canUseJsTag) {
                body.buildCatch(wasmFileCodegenContext.jsExceptionTagIndex)
            } else {
                body.buildCatchAll()
                body.buildRefNull(WasmHeapType.Simple.Extern, SourceLocation.NoLocation(""))
            }

            firstCatchBlock.wrapJsThrownValueIntoJsException()

            if (!areTwoCatchWithTheSameBody) {
                firstCatchBlock.initializeCatchParameter()
                generateExpression(firstCatchBlock.result)
            }
        }

        body.buildEnd() // try

        if (areTwoCatchWithTheSameBody) {
            lastCatchBlock.initializeCatchParameter()
            generateExpression(lastCatchBlock.result)
            body.buildEnd() // topLevelBlockLabel
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        when (expression.operator) {
            IrTypeOperator.REINTERPRET_CAST -> generateExpression(expression.argument)
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                generateAsStatement(expression.argument)
                body.buildGetUnit()
            }
            else -> assert(false) { "Other types of casts must be lowered" }
        }
    }

    override fun visitConst(expression: IrConst): Unit =
        generateConstExpression(expression, body, wasmFileCodegenContext, backendContext, expression.getSourceLocation())

    override fun visitGetField(expression: IrGetField) {
        val field: IrField = expression.symbol.owner
        val receiver: IrExpression? = expression.receiver
        val location = expression.getSourceLocation()

        if (receiver != null) {
            generateExpression(receiver)
            if (backendContext.inlineClassesUtils.isClassInlineLike(field.parentAsClass)) {
                // Unboxed inline class instance is already represented as backing field.
                // Doing nothing.
            } else {
                generateInstanceFieldAccess(field, location)
            }
        } else {
            body.buildGetGlobal(wasmFileCodegenContext.referenceGlobalField(field.symbol), location)
            body.commentPreviousInstr { "type: ${field.type.render()}" }
        }
    }

    private fun generateInstanceFieldAccess(field: IrField, location: SourceLocation) {
        val opcode = when (field.type) {
            irBuiltIns.charType ->
                WasmOp.STRUCT_GET_U

            irBuiltIns.booleanType,
            irBuiltIns.byteType,
            irBuiltIns.shortType ->
                WasmOp.STRUCT_GET_S

            else -> WasmOp.STRUCT_GET
        }

        body.buildInstr(
            opcode,
            location,
            WasmImmediate.GcType(wasmFileCodegenContext.referenceGcType(field.parentAsClass.symbol)),
            WasmImmediate.StructFieldIdx(getStructFieldRef(field))
        )
        body.commentPreviousInstr { "name: ${field.name.asString()}, type: ${field.type.render()}" }
    }

    override fun visitSetField(expression: IrSetField) {
        val field = expression.symbol.owner
        val receiver = expression.receiver

        val location = expression.getSourceLocation()

        if (receiver != null) {
            generateExpression(receiver)
            generateExpression(expression.value)
            body.buildStructSet(
                struct = wasmFileCodegenContext.referenceGcType(field.parentAsClass.symbol),
                fieldId = getStructFieldRef(field),
                location
            )
            body.commentPreviousInstr { "name: ${field.name}, type: ${field.type.render()}" }
        } else {
            generateExpression(expression.value)
            body.buildSetGlobal(wasmFileCodegenContext.referenceGlobalField(expression.symbol), location)
            body.commentPreviousInstr { "type: ${field.type.render()}" }
        }

        body.buildGetUnit()
    }

    override fun visitGetValue(expression: IrGetValue) {
        val valueSymbol = expression.symbol
        val valueDeclaration = valueSymbol.owner
        body.buildGetLocal(
            // Handle cases when IrClass::thisReceiver is referenced instead
            // of the value parameter of current function
            if (valueDeclaration.isDispatchReceiver)
                functionContext.referenceLocal(0)
            else
                functionContext.referenceLocal(valueSymbol),
            expression.getSourceLocation()
        )
        body.commentPreviousInstr { "type: ${valueDeclaration.type.render()}" }
    }

    override fun visitSetValue(expression: IrSetValue) {
        generateExpression(expression.value)
        body.buildSetLocal(functionContext.referenceLocal(expression.symbol), expression.getSourceLocation())
        body.commentPreviousInstr { "type: ${expression.symbol.owner.type.render()}" }
        body.buildGetUnit()
    }

    override fun visitCall(expression: IrCall) {
        generateCall(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        val klass: IrClass = expression.symbol.owner.parentAsClass
        val klassSymbol: IrClassSymbol = klass.symbol

        require(!backendContext.inlineClassesUtils.isClassInlineLike(klass)) {
            "All inline class constructor calls must be lowered to static function calls"
        }

        val wasmGcType: WasmSymbol<WasmTypeDeclaration> = wasmFileCodegenContext.referenceGcType(klassSymbol)
        val location = expression.getSourceLocation()

        if (klass.getWasmArrayAnnotation() != null) {
            require(expression.arguments.size == 1) { "@WasmArrayOf constructs must have exactly one argument" }
            generateExpression(expression.arguments[0]!!)
            body.buildInstr(
                WasmOp.ARRAY_NEW_DEFAULT,
                location,
                WasmImmediate.GcType(wasmGcType)
            )
            body.commentPreviousInstr { "@WasmArrayOf ctor call: ${klass.fqNameWhenAvailable}" }
            return
        }

        if (expression.symbol.owner.hasWasmPrimitiveConstructorAnnotation()) {
            generateAnyParameters(klassSymbol, location)
            expression.arguments.forEach { generateExpression(it!!) }

            body.buildStructNew(wasmGcType, location)
            body.commentPreviousInstr { "@WasmPrimitiveConstructor ctor call: ${klass.fqNameWhenAvailable}" }
            return
        }

        body.buildRefNull(WasmHeapType.Simple.None, location) // this = null
        generateCall(expression)
    }

    private fun generateAnyParameters(klassSymbol: IrClassSymbol, location: SourceLocation) {
        //ClassITable and VTable load
        body.commentGroupStart { "Any parameters" }
        body.buildGetGlobal(wasmFileCodegenContext.referenceGlobalVTable(klassSymbol), location)
        if (klassSymbol.owner.hasInterfaceSuperClass()) {
            body.buildGetGlobal(wasmFileCodegenContext.referenceGlobalClassITable(klassSymbol), location)
        } else {
            body.buildRefNull(WasmHeapType.Simple.None, location)
        }

        body.buildGetGlobal(wasmFileCodegenContext.referenceRttiGlobal(klassSymbol), location)
        body.buildConstI32(0, location) // Any::_hashCode
        body.commentGroupEnd()
    }

    fun generateObjectCreationPrefixIfNeeded(constructor: IrConstructor) {
        val parentClass = constructor.parentAsClass
        if (constructor.origin == PrimaryConstructorLowering.SYNTHETIC_PRIMARY_CONSTRUCTOR) return
        if (parentClass.isAbstractOrSealed) return
        val thisParameter = functionContext.referenceLocal(parentClass.thisReceiver!!.symbol)
        body.commentGroupStart { "Object creation prefix" }
        SourceLocation.NoLocation("Constructor preamble").let { location ->
            body.buildGetLocal(thisParameter, location)
            body.buildInstr(WasmOp.REF_IS_NULL, location)
            body.buildIf("this_init")
            generateAnyParameters(parentClass.symbol, location)
            val irFields: List<IrField> = parentClass.allFields(backendContext.irBuiltIns)
            irFields.forEachIndexed { index, field ->
                if (index > 0) {
                    generateDefaultInitializerForType(wasmModuleTypeTransformer.transformType(field.type), body)
                }
            }
            body.buildStructNew(wasmFileCodegenContext.referenceGcType(parentClass.symbol), location)
            body.buildSetLocal(thisParameter, location)
            body.buildEnd()
        }
        body.commentGroupEnd()
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        val klass = functionContext.irFunction!!.parentAsClass

        // Don't delegate constructors of Any to Any.
        if (klass.defaultType.isAny()) {
            body.buildGetUnit()
            return
        }

        body.buildGetLocal(functionContext.referenceLocal(0), SourceLocation.NoLocation("Get implicit dispatch receiver")) // this parameter
        generateCall(expression)
    }

    private fun generateBox(expression: IrExpression, type: IrType) {
        val klassSymbol = type.getRuntimeClass(irBuiltIns).symbol
        val location = expression.getSourceLocation()
        generateAnyParameters(klassSymbol, location)
        generateExpression(expression)
        body.buildStructNew(wasmFileCodegenContext.referenceGcType(klassSymbol), location)
        body.commentPreviousInstr { "box" }
    }

    private fun generateCall(call: IrFunctionAccessExpression) {
        val location = if (call.origin === IrStatementOrigin.DEFAULT_DISPATCH_CALL)
            SourceLocation.NoLocation("Default dispatch")
        else call.getSourceLocation()

        if (call.symbol == unitGetInstance.symbol) {
            body.buildGetUnit()
            return
        }

        // Box intrinsic has an additional klass ID argument.
        // Processing it separately
        if (call.symbol == wasmSymbols.boxBoolean) {
            generateBox(call.arguments[0]!!, irBuiltIns.booleanType)
            return
        }
        if (call.symbol == wasmSymbols.boxIntrinsic) {
            val type = call.typeArguments[0]!!
            if (type == irBuiltIns.booleanType) {
                generateExpression(call.arguments[0]!!)
                body.buildCall(wasmFileCodegenContext.referenceFunction(backendContext.wasmSymbols.getBoxedBoolean), location)
            } else {
                generateBox(call.arguments[0]!!, type)
            }
            return
        }

        if (call.symbol == wasmSymbols.wasmGetRttiIntField || call.symbol == wasmSymbols.wasmGetRttiLongField) {
            val fieldIndex = (call.arguments[0] as? IrConst)?.value as? Int ?: error("Invalid field index")
            generateExpression(call.arguments[1]!!)
            body.buildRefCastStatic(wasmFileCodegenContext.rttiType, location)
            body.buildStructGet(wasmFileCodegenContext.rttiType, WasmSymbol(fieldIndex), location)
            return
        }

        // Some intrinsics are a special case because we want to remove them completely, including their arguments.
        if (backendContext.configuration.get(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS) != true) {
            if (call.symbol == wasmSymbols.rangeCheck) {
                body.buildGetUnit()
                return
            }
        }
        if (backendContext.configuration.get(WasmConfigurationKeys.WASM_ENABLE_ASSERTS) != true) {
            if (call.symbol in wasmSymbols.asserts) {
                body.buildGetUnit()
                return
            }
        }

        call.arguments.forEach { generateExpression(it!!) }

        val callFunction = call.symbol.owner

        if (tryToGenerateIntrinsicCall(call, callFunction)) {
            if (callFunction.returnType.isUnit())
                body.buildGetUnit()
            return
        }

        // We skip now calling any ctor because it is empty
        if (callFunction.symbol.owner.hasWasmPrimitiveConstructorAnnotation()) return

        val function: IrFunction = callFunction.realOverrideTarget
        val isSuperCall = call is IrCall && call.superQualifierSymbol != null
        if (function is IrSimpleFunction && function.isOverridable && !isSuperCall) {
            val originalClass = callFunction.parentAsClass
            val realOverrideTargetClass = function.parentAsClass
            val klass = when {
                callFunction == function || !realOverrideTargetClass.isInterface || originalClass.isInterface -> realOverrideTargetClass
                else -> originalClass
            }

            val klassSymbol = klass.symbol
            val vTableGcTypeReference = wasmFileCodegenContext.referenceVTableGcType(klassSymbol)
            val functionTypeReference = wasmFileCodegenContext.referenceFunctionType(function.symbol)

            if (!klass.isInterface) {
                body.commentGroupStart { "Class Virtual call: ${function.fqNameWhenAvailable}" }

                val classMetadata = wasmModuleMetadataCache.getClassMetadata(klassSymbol)
                val vfSlot = classMetadata.virtualMethods.indexOfFirst { it.function == function }
                // Dispatch receiver should be simple and without side effects at this point
                // TODO: Verify
                val receiver = call.dispatchReceiver!!
                generateExpression(receiver)
                //TODO: check why it could be needed
                generateRefCast(receiver.type, klass.defaultType, isRefNullCast = false, location)

                body.buildStructGet(wasmFileCodegenContext.referenceGcType(klassSymbol), anyVtableFieldId, location)
                val vTableSlotId = WasmSymbol(vfSlot + 1) //First element is always contains Special ITable
                body.buildStructGet(vTableGcTypeReference, vTableSlotId, location)
                body.buildInstr(WasmOp.CALL_REF, location, WasmImmediate.TypeIdx(functionTypeReference))
            } else {
                generateExpression(call.dispatchReceiver!!)

                val specialITableSlot = backendContext.specialSlotITableTypes.indexOf(klassSymbol)
                if (specialITableSlot != -1) {
                    body.commentGroupStart { "Special Interface call: ${function.fqNameWhenAvailable}" }
                    generateSpecialITableFromAny(location)
                    body.buildStructGet(
                        wasmFileCodegenContext.interfaceTableTypes.specialSlotITableType,
                        WasmSymbol(specialITableSlot),
                        location
                    )
                } else if (klassSymbol.isFunction()) {
                    val functionalInterfaceSlot = getFunctionalInterfaceSlot(klass)

                    body.commentGroupStart { "Functional Interface call: ${function.fqNameWhenAvailable}" }
                    generateSpecialITableFromAny(location)
                    body.buildStructGet(
                        wasmFileCodegenContext.interfaceTableTypes.specialSlotITableType,
                        WasmSymbol(backendContext.specialSlotITableTypes.size),
                        location
                    )
                    body.buildConstI32(functionalInterfaceSlot, location)
                    body.buildInstr(
                        WasmOp.ARRAY_GET,
                        location,
                        WasmImmediate.TypeIdx(wasmFileCodegenContext.interfaceTableTypes.wasmAnyArrayType)
                    )
                } else {
                    body.commentGroupStart { "Interface call: ${function.fqNameWhenAvailable}" }
                    body.buildConstI64(wasmFileCodegenContext.referenceTypeId(klassSymbol), location)
                    body.buildCall(wasmFileCodegenContext.referenceFunction(wasmSymbols.reflectionSymbols.getInterfaceVTable), location)
                }

                body.buildRefCastStatic(vTableGcTypeReference, location)
                val vfSlot = wasmModuleMetadataCache.getInterfaceMetadata(klassSymbol).methods
                    .indexOfFirst { it.function == function }
                body.buildStructGet(vTableGcTypeReference, WasmSymbol(vfSlot), location)

                body.buildInstr(
                    WasmOp.CALL_REF,
                    location,
                    WasmImmediate.TypeIdx(functionTypeReference)
                )
            }
            body.commentGroupEnd()
        } else {
            // Static function call
            body.buildCall(wasmFileCodegenContext.referenceFunction(function.symbol), location)
        }

        // Unit types don't cross function boundaries
        if (function.returnType.isUnit() && function !is IrConstructor) {
            body.buildGetUnit()
        }
    }

    private fun generateRefCast(fromType: IrType, toType: IrType, isRefNullCast: Boolean, location: SourceLocation) {
        when {
            isDownCastAlwaysSuccessInRuntime(fromType, toType) -> {

            }
            isInvalidDownCast(fromType, toType) -> {
                body.buildUnreachable(location)
            }
            else -> {
                val wasmToType = wasmFileCodegenContext.referenceGcType(toType.getRuntimeClass(irBuiltIns).symbol)
                if (isRefNullCast) {
                    body.buildRefCastNullStatic(wasmToType, location)
                } else {
                    body.buildRefCastStatic(wasmToType, location)
                }
            }
        }
    }

    private fun generateRefTest(fromType: IrType, toType: IrType, location: SourceLocation) {
        when {
            isDownCastAlwaysSuccessInRuntime(fromType, toType) -> {
                body.buildDrop(location)
                body.buildConstI32(1, location)
            }
            isInvalidDownCast(fromType, toType) -> {
                body.buildUnreachable(location)
            }
            else -> {
                body.buildRefTestStatic(
                    toType = wasmFileCodegenContext.referenceGcType(toType.getRuntimeClass(irBuiltIns).symbol),
                    location
                )
            }
        }
    }

    private fun isInvalidDownCast(fromType: IrType, toType: IrType): Boolean {
        if (toType.isAny()) return false
        val fromTypeIsExternal = fromType.classOrNull?.owner?.isExternal ?: return false
        val toTypeIsExternal = toType.classOrNull?.owner?.isExternal ?: return false
        return fromTypeIsExternal != toTypeIsExternal
    }

    private fun isDownCastAlwaysSuccessInRuntime(fromType: IrType, toType: IrType): Boolean {
        val upperBound = fromType.erasedUpperBound
        if (upperBound.symbol.isSubtypeOfClass(backendContext.wasmSymbols.wasmAnyRefClass)) {
            return false
        }
        return fromType.getRuntimeClass(irBuiltIns).isSubclassOf(toType.getRuntimeClass(irBuiltIns))
    }

    private fun generateSpecialITableFromAny(location: SourceLocation) {
        body.buildStructGet(wasmFileCodegenContext.referenceGcType(irBuiltIns.anyClass), anyVtableFieldId, location)
        body.buildStructGet(wasmFileCodegenContext.referenceVTableGcType(irBuiltIns.anyClass), vTableSpecialITableFieldId, location)
    }

    // Return true if generated.
    // Assumes call arguments are already on the stack
    private fun tryToGenerateIntrinsicCall(
        call: IrFunctionAccessExpression,
        function: IrFunction,
    ): Boolean {
        if (tryToGenerateWasmOpIntrinsicCall(call, function)) {
            return true
        }

        val location = call.getSourceLocation()

        when (function.symbol) {
            wasmSymbols.wasmTypeId -> {
                val klass = call.typeArguments[0]!!.getClass()
                    ?: error("No class given for wasmTypeId intrinsic")
                body.buildConstI64(wasmFileCodegenContext.referenceTypeId(klass.symbol), location)
            }

            wasmSymbols.wasmGetTypeRtti -> {
                val klass = call.typeArguments[0]!!.getClass()
                    ?: error("No class given for wasmGetTypeRtti intrinsic")
                body.buildGetGlobal(wasmFileCodegenContext.referenceRttiGlobal(klass.symbol), location)
            }

            wasmSymbols.wasmGetRttiSupportedInterfaces -> {
                body.buildStructGet(wasmFileCodegenContext.referenceGcType(irBuiltIns.anyClass), anyRttiFieldId, location)
                body.buildStructGet(wasmFileCodegenContext.rttiType, rttiImplementedIFacesFieldId, location)
            }

            wasmSymbols.wasmGetRttiSuperClass -> {
                body.buildRefCastStatic(wasmFileCodegenContext.rttiType, location)
                body.buildStructGet(wasmFileCodegenContext.rttiType, rttiSuperClassFieldId, location)
            }

            wasmSymbols.reflectionSymbols.wasmGetInterfaceVTableBodyImpl -> {
                //This is implementation of getInterfaceVTable, so argument locals could be used from the call-site
                //obj.interfacesArray
                body.buildGetLocal(functionContext.referenceLocal(0), location) //obj
                body.buildStructGet(wasmFileCodegenContext.referenceGcType(irBuiltIns.anyClass), anyITableFieldId, location)

                //wasmArrayAnyIndexOfValue(obj.rtti.interfaceIds)
                body.buildGetLocal(functionContext.referenceLocal(0), location) //obj
                body.buildStructGet(wasmFileCodegenContext.referenceGcType(irBuiltIns.anyClass), anyRttiFieldId, location)
                body.buildStructGet(wasmFileCodegenContext.rttiType, rttiImplementedIFacesFieldId, location)
                body.buildGetLocal(functionContext.referenceLocal(1), location) //interfaceId
                body.buildCall(wasmFileCodegenContext.referenceFunction(wasmSymbols.wasmArrayAnyIndexOfValue), location)

                body.buildInstr(
                    WasmOp.ARRAY_GET,
                    location,
                    WasmImmediate.TypeIdx(wasmFileCodegenContext.interfaceTableTypes.wasmAnyArrayType)
                )
            }

            wasmSymbols.wasmGetObjectRtti -> {
                body.buildStructGet(wasmFileCodegenContext.referenceGcType(irBuiltIns.anyClass), anyRttiFieldId, location)
            }

            wasmSymbols.wasmIsInterface -> {
                val irInterface = call.typeArguments[0]!!.getClass()
                    ?: error("No interface given for wasmIsInterface intrinsic")
                assert(irInterface.isInterface)

                val specialSlotIndex = backendContext.specialSlotITableTypes.indexOf(irInterface.symbol)
                if (specialSlotIndex != -1) {
                    body.commentGroupStart { "Check special interface supported" }
                    body.buildSetLocal(functionContext.referenceLocal(SyntheticLocalType.IS_INTERFACE_PARAMETER), location)
                    body.buildBlock("SpecialIFaceTestSuccess", WasmI32) { success ->
                        body.buildBlock("SpecialIFaceTestFail") { fail ->
                            body.buildGetLocal(functionContext.referenceLocal(SyntheticLocalType.IS_INTERFACE_PARAMETER), location)
                            generateSpecialITableFromAny(location)

                            body.buildBrInstr(WasmOp.BR_ON_NULL, fail, location)
                            body.buildStructGet(
                                wasmFileCodegenContext.interfaceTableTypes.specialSlotITableType,
                                WasmSymbol(specialSlotIndex),
                                location
                            )
                            body.buildInstr(WasmOp.REF_IS_NULL, location)
                            body.buildInstr(WasmOp.I32_EQZ, location)
                            body.buildBr(success, location)
                        }
                        body.buildConstI32(0, location)
                    }
                } else {
                    if (irInterface.symbol.isFunction()) {
                        val functionalInterfaceSlot = getFunctionalInterfaceSlot(irInterface)

                        body.commentGroupStart { "Check functional interface supported" }
                        body.buildSetLocal(functionContext.referenceLocal(SyntheticLocalType.IS_INTERFACE_PARAMETER), location)
                        body.buildBlock("FunctionTestSuccess", WasmI32) { result ->
                            body.buildBlock("FunctionTestFail") { fail ->
                                body.buildGetLocal(functionContext.referenceLocal(SyntheticLocalType.IS_INTERFACE_PARAMETER), location)
                                generateSpecialITableFromAny(location)

                                body.buildBrInstr(WasmOp.BR_ON_NULL, fail, location)
                                body.buildStructGet(
                                    wasmFileCodegenContext.interfaceTableTypes.specialSlotITableType,
                                    WasmSymbol(backendContext.specialSlotITableTypes.size),
                                    location
                                )
                                body.buildBrInstr(WasmOp.BR_ON_NULL, fail, location)

                                body.buildTeeLocal(functionContext.referenceLocal(SyntheticLocalType.IS_INTERFACE_ANY_ARRAY), location)

                                body.buildInstr(WasmOp.ARRAY_LEN, location)

                                body.buildConstI32(functionalInterfaceSlot, location)

                                body.buildInstr(WasmOp.I32_LE_U, location)
                                body.buildBrIf(fail, location)

                                body.buildGetLocal(functionContext.referenceLocal(SyntheticLocalType.IS_INTERFACE_ANY_ARRAY), location)
                                body.buildConstI32(functionalInterfaceSlot, location)

                                body.buildInstr(
                                    WasmOp.ARRAY_GET,
                                    location,
                                    WasmImmediate.TypeIdx(wasmFileCodegenContext.interfaceTableTypes.wasmAnyArrayType)
                                )
                                body.buildInstr(WasmOp.REF_IS_NULL, location)
                                body.buildInstr(WasmOp.I32_EQZ, location)
                                body.buildBr(result, location)
                            }
                            body.buildConstI32(0, location)
                        }
                    } else {
                        body.commentGroupStart { "Check interface supported" }
                        body.buildConstI64(wasmFileCodegenContext.referenceTypeId(irInterface.symbol), location)
                        body.buildCall(wasmFileCodegenContext.referenceFunction(wasmSymbols.reflectionSymbols.isSupportedInterface), location)
                    }
                }
                body.commentGroupEnd()
            }
            wasmSymbols.refCastNull -> {
                generateRefCast(
                    fromType = call.arguments[0]!!.type,
                    toType = call.typeArguments[0]!!,
                    isRefNullCast = true,
                    location = location,
                )
            }

            wasmSymbols.refTest -> {
                generateRefTest(
                    fromType = call.arguments[0]!!.type,
                    toType = call.typeArguments[0]!!,
                    location
                )
            }

            wasmSymbols.unboxIntrinsic -> {
                val fromType = call.typeArguments[0]!!

                if (fromType.isNothing()) {
                    body.buildUnreachableAfterNothingType()
                    // TODO: Investigate why?
                    return true
                }

                val toType = call.typeArguments[1]!!
                val klass: IrClass = backendContext.inlineClassesUtils.getInlinedClass(toType)!!
                val field = getInlineClassBackingField(klass)

                generateRefCast(fromType, toType, isRefNullCast = false, location)
                generateInstanceFieldAccess(field, location)
            }

            wasmSymbols.returnArgumentIfItIsKotlinAny -> {
                body.buildBlock("returnIfAny", WasmAnyRef) { innerLabel ->
                    body.buildGetLocal(functionContext.referenceLocal(0), location)
                    body.buildInstr(WasmOp.EXTERN_INTERNALIZE, location)

                    body.buildBrOnCastInstr(
                        WasmOp.BR_ON_CAST_FAIL,
                        innerLabel,
                        fromIsNullable = true,
                        toIsNullable = true,
                        from = WasmHeapType.Simple.Any,
                        to = WasmHeapType.Type(wasmFileCodegenContext.referenceGcType(backendContext.irBuiltIns.anyClass)),
                        location,
                    )

                    body.buildInstr(WasmOp.RETURN, location)
                }
                body.buildDrop(location)
            }

            wasmSymbols.wasmArrayCopy -> {
                val immediate = WasmImmediate.GcType(
                    wasmFileCodegenContext.referenceGcType(call.typeArguments[0]!!.getRuntimeClass(irBuiltIns).symbol)
                )
                body.buildInstr(WasmOp.ARRAY_COPY, location, immediate, immediate)
            }

            wasmSymbols.stringGetPoolSize -> {
                body.buildConstI32Symbol(wasmFileCodegenContext.stringPoolSize, location)
            }

            wasmSymbols.wasmArrayNewData0 -> {
                val arrayGcType = WasmImmediate.GcType(
                    wasmFileCodegenContext.referenceGcType(call.typeArguments[0]!!.getRuntimeClass(irBuiltIns).symbol)
                )
                body.buildInstr(WasmOp.ARRAY_NEW_DATA, location, arrayGcType, WasmImmediate.DataIdx(0))
            }

            else -> {
                return false
            }
        }

        return true
    }

    override fun visitBlockBody(body: IrBlockBody) {
        body.statements.forEach(::generateStatement)
        this.body.buildNop(body.getSourceEndLocation())
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock) {
        val inlineFunction = inlinedBlock.inlinedFunctionSymbol?.owner
        val correspondingProperty = (inlineFunction as? IrSimpleFunction)?.correspondingPropertySymbol
        val owner = correspondingProperty?.owner ?: inlineFunction
        val name = owner?.fqNameWhenAvailable?.asString() ?: owner?.name?.asString() ?: "UNKNOWN"

        body.commentGroupStart { "Inlined call of `$name`" }
        body.buildNop(inlinedBlock.getSourceLocation())

        functionContext.stepIntoInlinedFunction(inlinedBlock.inlinedFunctionSymbol, inlinedBlock.inlinedFunctionFileEntry)
        super.visitInlinedFunctionBlock(inlinedBlock)
        functionContext.stepOutLastInlinedFunction()
    }

    override fun visitReturnableBlock(expression: IrReturnableBlock) {
        functionContext.defineNonLocalReturnLevel(
            expression.symbol,
            body.buildBlock(wasmModuleTypeTransformer.transformBlockResultType(expression.type))
        )
        super.visitReturnableBlock(expression)
    }

    private fun processContainerExpression(expression: IrContainerExpression) {
        val statements = expression.statements
        statements.forEachIndexed { i, statement ->
            if (i != statements.lastIndex) {
                generateStatement(statement)
            } else {
                if (statement is IrExpression) {
                    generateWithExpectedType(statement, expression.type)
                } else {
                    generateStatement(statement)
                    if (expression.type != wasmSymbols.voidType) {
                        body.buildGetUnit()
                    }
                }
            }
        }

        if (expression is IrReturnableBlock) {
            body.buildEnd()
            body.commentGroupEnd()
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        if (expression.statements.isEmpty()) {
            if (expression.type == irBuiltIns.unitType) {
                body.buildGetUnit()
            }
            return
        }

        processContainerExpression(expression)
    }

    override fun visitBreak(jump: IrBreak) {
        assert(jump.type == irBuiltIns.nothingType)
        body.buildBr(functionContext.referenceLoopLevel(jump.loop, LoopLabelType.BREAK), jump.getSourceLocation())
    }

    override fun visitContinue(jump: IrContinue) {
        assert(jump.type == irBuiltIns.nothingType)
        body.buildBr(functionContext.referenceLoopLevel(jump.loop, LoopLabelType.CONTINUE), jump.getSourceLocation())
    }

    private fun visitFunctionReturn(expression: IrReturn) {
        val returnType = expression.returnTargetSymbol.owner.returnType(backendContext)
        val isGetUnitFunction = expression.returnTargetSymbol.owner == unitGetInstance

        when {
            isGetUnitFunction -> generateExpression(expression.value)
            returnType == irBuiltIns.unitType -> generateAsStatement(expression.value)
            else -> generateWithExpectedType(expression.value, returnType)
        }

        if (functionContext.irFunction is IrConstructor) {
            body.buildGetLocal(functionContext.referenceLocal(0), SourceLocation.NoLocation("Get implicit dispatch receiver"))
        }

        body.buildInstr(WasmOp.RETURN, expression.getSourceLocation())
    }

    internal fun generateWithExpectedType(expression: IrExpression, expectedType: IrType) {
        val actualType = expression.type

        if (expectedType == wasmSymbols.voidType) {
            generateAsStatement(expression)
            return
        }

        if (expectedType.isUnit() && !actualType.isUnit()) {
            generateAsStatement(expression)
            body.buildGetUnit()
            return
        }

        generateExpression(expression)
        recoverToExpectedType(actualType = actualType, expectedType = expectedType, location = expression.getSourceLocation())
    }

    //TODO: This method needed because of IR has type inconsistency. We need to discover why is it and fix
    private fun recoverToExpectedType(actualType: IrType, expectedType: IrType, location: SourceLocation) {
        // TYPE -> NOTHING -> FALSE
        if (expectedType.isNothing()) {
            body.buildUnreachableAfterNothingType()
            return
        }

        // NOTHING -> TYPE -> TRUE
        if (actualType.isNothing()) return

        // NOTHING? -> TYPE? -> (NOTHING?)NULL
        if (actualType.isNullableNothing() && expectedType.isNullable()) {
            if (expectedType.getClass()?.isExternal == true) {
                body.buildDrop(location)
                body.buildRefNull(WasmHeapType.Simple.NoExtern, location)
            }
            return
        }

        // Type? -> Nothing? -> ref.cast null (none/noextern)
        if (actualType.isNullable() && expectedType.isNullableNothing()) {
            val type =
                if (expectedType.getClass()?.isExternal == true)
                    WasmHeapType.Simple.NoExtern
                else
                    WasmHeapType.Simple.None

            body.buildInstr(WasmOp.REF_CAST_NULL, location, WasmImmediate.HeapType(type))

            return
        }

        val expectedClassErased = expectedType.getRuntimeClass(irBuiltIns)

        // TYPE -> EXTERNAL -> TRUE
        if (expectedClassErased.isExternal) return

        val actualClassErased = actualType.getRuntimeClass(irBuiltIns)
        val expectedTypeErased = expectedClassErased.defaultType
        val actualTypeErased = actualClassErased.defaultType

        // TYPE -> TYPE -> TRUE
        if (expectedTypeErased == actualTypeErased) return

        // NOT_NOTHING_TYPE -> NOTHING -> FALSE
        if (expectedTypeErased.isNothing() && !actualTypeErased.isNothing()) {
            body.buildUnreachableAfterNothingType()
            return
        }

        // TYPE -> BASE -> TRUE
        // TODO Shouldn't we keep nullability for subtype check?
        if (actualClassErased.isSubclassOf(expectedClassErased)) {
            return
        }

        val expectedIsPrimitive = expectedTypeErased.isPrimitiveType() && !expectedType.isNullable()
        val actualIsPrimitive = actualTypeErased.isPrimitiveType() && !actualType.isNullable()

        // PRIMITIVE -> REF -> FALSE
        // REF -> PRIMITIVE -> FALSE
        if (expectedIsPrimitive != actualIsPrimitive) {
            // TODO Shouldn't we throw ICE instead?
            body.buildUnreachableForVerifier()
            return
        }

        // REF -> REF -> REF_CAST
        if (!expectedIsPrimitive) {
            if (expectedClassErased.isSubclassOf(actualClassErased)) {
                generateRefCast(actualTypeErased, expectedTypeErased, isRefNullCast = expectedType.isNullable(), location)
                body.commentPreviousInstr { "to make verifier happy" }
            } else {
                body.buildUnreachableForVerifier()
            }
        }
    }

    override fun visitReturn(expression: IrReturn) {
        val nonLocalReturnSymbol = expression.returnTargetSymbol as? IrReturnableBlockSymbol
        if (nonLocalReturnSymbol != null) {
            generateWithExpectedType(expression.value, nonLocalReturnSymbol.owner.type)
            body.buildBr(functionContext.referenceNonLocalReturnLevel(nonLocalReturnSymbol), expression.getSourceLocation())
        } else {
            visitFunctionReturn(expression)
        }
    }

    override fun visitWhen(expression: IrWhen) {
        if (!backendContext.isDebugFriendlyCompilation && tryGenerateOptimisedWhen(
                expression,
                backendContext.wasmSymbols,
                functionContext,
                wasmModuleTypeTransformer
            )
        ) {
            return
        }

        val branches = expression.branches
        val onlyOneBranch = branches.singleOrNull()

        if (onlyOneBranch != null && isElseBranch(onlyOneBranch)) {
            generateExpression(onlyOneBranch.result)
            return
        }

        val resultType = wasmModuleTypeTransformer.transformBlockResultType(expression.type)
        var ifCount = 0
        var seenElse = false
        val isLogicalOperator = expression.origin == IrStatementOrigin.ANDAND || expression.origin == IrStatementOrigin.OROR
        val expressionLocation = expression.takeIf { isLogicalOperator }?.getSourceLocation()

        for (branch in branches) {
            if (!isElseBranch(branch)) {
                if (ifCount > 0) body.buildElse()
                generateExpression(branch.condition)
                body.buildIf(null, resultType)
                generateWithExpectedType(branch.result, expression.type)
                ifCount++
            } else {
                body.buildElse(expressionLocation)
                generateWithExpectedType(branch.result, expression.type)
                seenElse = true
                break
            }
        }

        // Always generate the last else to make verifier happy. If this when expression is exhaustive we will never reach the last else.
        // If it's not exhaustive it must be used as a statement (per kotlin spec) and so the result value of the last else will never be used.
        if (!seenElse && resultType != null) {
            assert(expression.type != irBuiltIns.nothingType)
            if (expression.type.isUnit()) {
                if (branches.isNotEmpty()) body.buildElse()
                body.buildGetUnit()
            } else {
                error("'When' without else branch and non Unit type: ${expression.type.dumpKotlinLike()}")
            }
        }

        repeat(ifCount) {
            val endLocation = branches[branches.lastIndex - it].takeIf { !isLogicalOperator }?.nextLocation()
            body.buildEnd(endLocation)
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        // (loop $LABEL
        //     (block $BREAK_LABEL
        //         (block $CONTINUE_LABEL <LOOP BODY>)
        //         (br_if $LABEL          <CONDITION>)))

        val label = loop.label

        body.buildLoop(label) { wasmLoop ->
            body.buildBlock("BREAK_$label") { wasmBreakBlock ->
                body.buildBlock("CONTINUE_$label") { wasmContinueBlock ->
                    functionContext.defineLoopLevel(loop, LoopLabelType.BREAK, wasmBreakBlock)
                    functionContext.defineLoopLevel(loop, LoopLabelType.CONTINUE, wasmContinueBlock)
                    loop.body?.let { generateAsStatement(it) }
                }
                generateExpression(loop.condition)
                body.buildBrIf(wasmLoop, loop.condition.getSourceLocation())
            }
        }

        body.buildGetUnit()
    }

    override fun visitWhileLoop(loop: IrWhileLoop) {
        // (loop $CONTINUE_LABEL
        //     (block $BREAK_LABEL
        //         (br_if $BREAK_LABEL (i32.eqz <CONDITION>))
        //         <LOOP_BODY>
        //         (br $CONTINUE_LABEL)))

        val label = loop.label

        body.buildLoop(label) { wasmLoop ->
            body.buildBlock("BREAK_$label") { wasmBreakBlock ->
                functionContext.defineLoopLevel(loop, LoopLabelType.BREAK, wasmBreakBlock)
                functionContext.defineLoopLevel(loop, LoopLabelType.CONTINUE, wasmLoop)

                generateExpression(loop.condition)
                val location = loop.condition.getSourceLocation()
                body.buildInstr(WasmOp.I32_EQZ, location)
                body.buildBrIf(wasmBreakBlock, location)
                loop.body?.let {
                    generateAsStatement(it)
                }
                body.buildBr(wasmLoop, SourceLocation.NoLocation("Continue in the loop"))
            }
        }

        body.buildGetUnit()
    }

    override fun visitVariable(declaration: IrVariable) {
        functionContext.defineLocal(declaration.symbol)
        if (declaration.initializer == null) {
            return
        }
        val init = declaration.initializer!!
        generateExpression(init)
        val varName = functionContext.referenceLocal(declaration.symbol)
        body.buildSetLocal(varName, init.getSourceLocation())
    }

    // Return true if function is recognized as intrinsic.
    private fun tryToGenerateWasmOpIntrinsicCall(call: IrFunctionAccessExpression, function: IrFunction): Boolean {
        if (function.hasWasmNoOpCastAnnotation()) {
            return true
        }

        val opString = function.getWasmOpAnnotation()
        if (opString != null) {
            val location = call.getSourceLocation()
            val op = WasmOp.valueOf(opString)
            when (op.immediates.size) {
                0 -> {
                    body.buildInstr(op, location)
                }
                1 -> {
                    fun getReferenceGcType(): WasmSymbol<WasmTypeDeclaration> {
                        val type = function.dispatchReceiverParameter?.type ?: call.typeArguments[0]!!
                        return wasmFileCodegenContext.referenceGcType(type.classOrNull!!)
                    }

                    val immediates = arrayOf(
                        when (val imm = op.immediates[0]) {
                            WasmImmediateKind.MEM_ARG ->
                                WasmImmediate.MemArg(0u, 0u)
                            WasmImmediateKind.STRUCT_TYPE_IDX ->
                                WasmImmediate.GcType(getReferenceGcType())
                            WasmImmediateKind.HEAP_TYPE ->
                                WasmImmediate.HeapType(WasmHeapType.Type(getReferenceGcType()))
                            WasmImmediateKind.TYPE_IDX ->
                                WasmImmediate.TypeIdx(getReferenceGcType())
                            WasmImmediateKind.MEMORY_IDX ->
                                WasmImmediate.MemoryIdx(0)

                            else ->
                                error("Immediate $imm is unsupported")
                        }
                    )
                    body.buildInstr(op, location, *immediates)
                }
                else ->
                    error("Op $opString is unsupported")
            }
            return true
        }

        return false
    }

    private fun IrElement.getSourceLocation() = getSourceLocation(
        functionContext.currentFunctionSymbol, functionContext.currentFunctionSymbol?.owner?.fileOrNull
    )

    private fun IrElement.getSourceEndLocation() = getSourceLocation(
        functionContext.currentFunctionSymbol, functionContext.currentFunctionSymbol?.owner?.fileOrNull, type = LocationType.END
    )

    private fun IrElement.nextLocation() = when (getSourceLocation()) {
        is SourceLocation.DefinedLocation -> SourceLocation.NextLocation
        else -> SourceLocation.NoLocation
    }

    companion object {
        val anyVtableFieldId = WasmSymbol(0)
        val anyITableFieldId = WasmSymbol(1)
        val anyRttiFieldId = WasmSymbol(2)
        val vTableSpecialITableFieldId = WasmSymbol(0)
        val rttiImplementedIFacesFieldId = WasmSymbol(0)
        val rttiSuperClassFieldId = WasmSymbol(1)
    }
}
