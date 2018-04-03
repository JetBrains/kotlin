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

package org.jetbrains.kotlin.backend.konan.llvm


import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.descriptors.stdlibModule
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.ObjCDataGenerator
import org.jetbrains.kotlin.konan.target.KonanTarget

internal class CodeGenerator(override val context: Context) : ContextUtils {

    fun llvmFunction(function: FunctionDescriptor): LLVMValueRef = function.llvmFunction
    val intPtrType = LLVMIntPtrType(llvmTargetData)!!
    internal val immOneIntPtrType = LLVMConstInt(intPtrType, 1, 1)!!

    //-------------------------------------------------------------------------//

    /* to class descriptor */
    fun typeInfoValue(descriptor: ClassDescriptor): LLVMValueRef = descriptor.llvmTypeInfoPtr

    fun param(fn: FunctionDescriptor, i: Int): LLVMValueRef {
        assert(i >= 0 && i < countParams(fn))
        return LLVMGetParam(fn.llvmFunction, i)!!
    }

    private fun countParams(fn: FunctionDescriptor) = LLVMCountParams(fn.llvmFunction)

    fun functionEntryPointAddress(descriptor: FunctionDescriptor) = descriptor.entryPointAddress.llvm
    fun functionHash(descriptor: FunctionDescriptor): LLVMValueRef = descriptor.functionName.localHash.llvm

    fun getObjectInstanceStorage(descriptor: ClassDescriptor, shared: Boolean): LLVMValueRef {
        assert (!descriptor.isUnit())
        val llvmGlobal = if (!isExternal(descriptor)) {
            context.llvmDeclarations.forSingleton(descriptor).instanceFieldRef
        } else {
            val llvmType = getLLVMType(descriptor.defaultType)
            importGlobal(
                    descriptor.objectInstanceFieldSymbolName,
                    llvmType,
                    origin = descriptor.llvmSymbolOrigin,
                    threadLocal = !shared
            )
        }
        if (shared)
            context.llvm.sharedObjects += llvmGlobal
        else
            context.llvm.objects += llvmGlobal
        return llvmGlobal
    }

    fun getObjectInstanceShadowStorage(descriptor: ClassDescriptor): LLVMValueRef {
        assert (!descriptor.isUnit())
        assert (descriptor.symbol.objectIsShared)
        val llvmGlobal = if (!isExternal(descriptor)) {
            context.llvmDeclarations.forSingleton(descriptor).instanceShadowFieldRef!!
        } else {
            val llvmType = getLLVMType(descriptor.defaultType)
            importGlobal(
                    descriptor.objectInstanceShadowFieldSymbolName,
                    llvmType,
                    origin = descriptor.llvmSymbolOrigin,
                    threadLocal = true
            )
        }
        context.llvm.objects += llvmGlobal
        return llvmGlobal
    }

    fun typeInfoForAllocation(constructedClass: ClassDescriptor): LLVMValueRef {
        assert(!constructedClass.isObjCClass())
        return typeInfoValue(constructedClass)
    }

    fun generateLocationInfo(locationInfo: LocationInfo): DILocationRef? {
        return LLVMCreateLocation(LLVMGetModuleContext(context.llvmModule), locationInfo.line, locationInfo.line, locationInfo.scope)
    }

    val objCDataGenerator = when (context.config.target) {
        KonanTarget.IOS_ARM64, KonanTarget.IOS_X64, KonanTarget.MACOS_X64 -> ObjCDataGenerator(this)
        else -> null
    }

}

internal sealed class ExceptionHandler {
    object None : ExceptionHandler()
    object Caller : ExceptionHandler()
    abstract class Local : ExceptionHandler() {
        abstract val unwind: LLVMBasicBlockRef
    }
}

val LLVMValueRef.name:String?
    get() = LLVMGetValueName(this)?.toKString()

val LLVMValueRef.isConst:Boolean
    get() = (LLVMIsConstant(this) == 1)


internal inline fun<R> generateFunction(codegen: CodeGenerator,
                                        descriptor: FunctionDescriptor,
                                        startLocation: LocationInfo? = null,
                                        endLocation: LocationInfo? = null,
                                        code:FunctionGenerationContext.(FunctionGenerationContext) -> R) {
    val llvmFunction = codegen.llvmFunction(descriptor)

    generateFunctionBody(FunctionGenerationContext(
            llvmFunction,
            codegen,
            startLocation,
            endLocation,
            descriptor), code)
}


internal inline fun<R> generateFunction(codegen: CodeGenerator, function: LLVMValueRef, code:FunctionGenerationContext.(FunctionGenerationContext) -> R) {
    generateFunctionBody(FunctionGenerationContext(function, codegen), code)
}

internal inline fun generateFunction(
        codegen: CodeGenerator,
        functionType: LLVMTypeRef,
        name: String,
        block: FunctionGenerationContext.(FunctionGenerationContext) -> Unit
): LLVMValueRef {
    val function = LLVMAddFunction(codegen.context.llvmModule, name, functionType)!!
    generateFunction(codegen, function, block)
    return function
}

inline private fun <R> generateFunctionBody(functionGenerationContext: FunctionGenerationContext, code: FunctionGenerationContext.(FunctionGenerationContext) -> R) {
    functionGenerationContext.prologue()
    functionGenerationContext.code(functionGenerationContext)
    if (!functionGenerationContext.isAfterTerminator())
        functionGenerationContext.unreachable()
    functionGenerationContext.epilogue()
    functionGenerationContext.resetDebugLocation()
}

internal class FunctionGenerationContext(val function: LLVMValueRef,
                                         val codegen:CodeGenerator,
                                         startLocation:LocationInfo? = null,
                                         endLocation:LocationInfo? = null,
                                         internal val functionDescriptor: FunctionDescriptor? = null):ContextUtils {
    override val context = codegen.context
    val vars = VariableManager(this)
    private val basicBlockToLastLocation = mutableMapOf<LLVMBasicBlockRef, LocationInfo>()

    private fun update(block:LLVMBasicBlockRef, locationInfo: LocationInfo?) {
        locationInfo ?: return
        basicBlockToLastLocation.put(block, locationInfo)
    }

    var returnType: LLVMTypeRef? = LLVMGetReturnType(getFunctionType(function))
    private val returns: MutableMap<LLVMBasicBlockRef, LLVMValueRef> = mutableMapOf()
    // TODO: remove, to make CodeGenerator descriptor-agnostic.
    val constructedClass: ClassDescriptor?
        get() = (functionDescriptor as? ClassConstructorDescriptor)?.constructedClass
    private var returnSlot: LLVMValueRef? = null
    private var slotsPhi: LLVMValueRef? = null
    private val frameOverlaySlotCount =
            (LLVMStoreSizeOfType(llvmTargetData, runtime.frameOverlayType) / runtime.pointerSize).toInt()
    private var slotCount = frameOverlaySlotCount
    private var localAllocs = 0
    private var arenaSlot: LLVMValueRef? = null
    private val slotToVariableLocation = mutableMapOf<Int,VariableDebugLocation>()

    private val prologueBb        = basicBlockInFunction("prologue", startLocation)
    private val localsInitBb      = basicBlockInFunction("locals_init", startLocation)
    private val entryBb           = basicBlockInFunction("entry", startLocation)
    private val epilogueBb        = basicBlockInFunction("epilogue", endLocation)
    private val cleanupLandingpad = basicBlockInFunction("cleanup_landingpad", endLocation)

    /**
     * TODO: consider merging this with [ExceptionHandler].
     */
    var forwardingForeignExceptionsTerminatedWith: LLVMValueRef? = null

    init {
        functionDescriptor?.let {
            if (!functionDescriptor.isExported()) {
                LLVMSetLinkage(function, LLVMLinkage.LLVMInternalLinkage)
                // (Cannot do this before the function body is created).
            }
        }
    }

    private fun basicBlockInFunction(name: String, locationInfo: LocationInfo?): LLVMBasicBlockRef {
        val bb = LLVMAppendBasicBlock(function, name)!!
        update(bb, locationInfo)
        return bb
    }

    fun basicBlock(name:String = "label_" , locationInfo:LocationInfo?):LLVMBasicBlockRef {
        val result = LLVMInsertBasicBlock(this.currentBlock, name)!!
        update(result, locationInfo)
        LLVMMoveBasicBlockAfter(result, this.currentBlock)
        return result
    }

    fun alloca(type: LLVMTypeRef?, name: String = "", variableLocation: VariableDebugLocation? = null): LLVMValueRef {
        if (isObjectType(type!!)) {
            appendingTo(localsInitBb) {
                val slotAddress = gep(slotsPhi!!, Int32(slotCount).llvm, name)
                variableLocation?.let {
                    slotToVariableLocation[slotCount] = it
                }
                slotCount++
                return slotAddress
            }
        }

        appendingTo(prologueBb) {
            val slotAddress = LLVMBuildAlloca(builder, type, name)!!
            variableLocation?.let {
                DIInsertDeclaration(
                        builder       = codegen.context.debugInfo.builder,
                        value         = slotAddress,
                        localVariable = it.localVariable,
                        location      = it.location,
                        bb            = prologueBb,
                        expr          = null,
                        exprCount     = 0)
            }
            return slotAddress
        }
    }


    fun ret(value: LLVMValueRef?): LLVMValueRef {
        val res = LLVMBuildBr(builder, epilogueBb)!!
        if (returns.containsKey(currentBlock)) {
            // TODO: enable error throwing.
            throw Error("ret() in the same basic block twice! in ${function.name}")
        }

        if (value != null)
            returns[currentBlock] = value

        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun param(index: Int): LLVMValueRef = LLVMGetParam(this.function, index)!!

    fun load(value: LLVMValueRef, name: String = ""): LLVMValueRef {
        val result = LLVMBuildLoad(builder, value, name)!!
        // Use loadSlot() API for that.
        assert(!isObjectRef(value))
        return result
    }

    fun loadSlot(address: LLVMValueRef, isVar: Boolean, name: String = ""): LLVMValueRef {
        val value = LLVMBuildLoad(builder, address, name)!!
        if (isObjectRef(value) && isVar) {
            val slot = alloca(LLVMTypeOf(value), variableLocation = null)
            storeAny(value, slot)
        }
        return value
    }

    fun store(value: LLVMValueRef, ptr: LLVMValueRef) {
        // Use updateRef() or storeAny() API for that.
        assert(!isObjectRef(value))
        LLVMBuildStore(builder, value, ptr)
    }

    fun storeAny(value: LLVMValueRef, ptr: LLVMValueRef) {
        if (isObjectRef(value)) {
            updateRef(value, ptr)
        } else {
            LLVMBuildStore(builder, value, ptr)
        }
    }

    private fun updateReturnRef(value: LLVMValueRef, address: LLVMValueRef) {
        call(context.llvm.updateReturnRefFunction, listOf(address, value))
    }

    private fun updateRef(value: LLVMValueRef, address: LLVMValueRef) {
        call(context.llvm.updateRefFunction, listOf(address, value))
    }

    //-------------------------------------------------------------------------//

    fun call(llvmFunction: LLVMValueRef, args: List<LLVMValueRef>,
             resultLifetime: Lifetime = Lifetime.IRRELEVANT,
             exceptionHandler: ExceptionHandler = ExceptionHandler.None,
             verbatim: Boolean = false): LLVMValueRef {
        val callArgs = if (verbatim || !isObjectReturn(llvmFunction.type)) {
            args
        } else {
            // If function returns an object - create slot for the returned value or give local arena.
            // This allows appropriate rootset accounting by just looking at the stack slots,
            // along with ability to allocate in appropriate arena.
            val resultSlot = when (resultLifetime.slotType) {
                SlotType.ARENA -> {
                    localAllocs++
                    arenaSlot!!
                }

                SlotType.RETURN -> returnSlot!!

                SlotType.ANONYMOUS -> vars.createAnonymousSlot()

                SlotType.RETURN_IF_ARENA -> returnSlot.let {
                    if (it != null)
                        call(context.llvm.getReturnSlotIfArenaFunction, listOf(it, vars.createAnonymousSlot()))
                    else {
                        // Return type is not an object type - can allocate locally.
                        localAllocs++
                        arenaSlot!!
                    }
                }

                is SlotType.PARAM_IF_ARENA ->
                    if (LLVMTypeOf(vars.load(resultLifetime.slotType.parameter)) != codegen.runtime.objHeaderPtrType)
                        vars.createAnonymousSlot()
                    else {
                        call(context.llvm.getParamSlotIfArenaFunction,
                                listOf(vars.load(resultLifetime.slotType.parameter), vars.createAnonymousSlot()))
                    }

                else -> throw Error("Incorrect slot type: ${resultLifetime.slotType}")
            }
            args + resultSlot
        }
        return callRaw(llvmFunction, callArgs, exceptionHandler)
    }

    private fun callRaw(llvmFunction: LLVMValueRef, args: List<LLVMValueRef>,
                        exceptionHandler: ExceptionHandler): LLVMValueRef {
        val rargs = args.toCValues()
        if (LLVMIsAFunction(llvmFunction) != null /* the function declaration */  &&
                isFunctionNoUnwind(llvmFunction)) {

            return LLVMBuildCall(builder, llvmFunction, rargs, args.size, "")!!
        } else {
            val unwind = when (exceptionHandler) {
                ExceptionHandler.Caller -> cleanupLandingpad
                is ExceptionHandler.Local -> exceptionHandler.unwind

                ExceptionHandler.None -> {
                    // When calling a function that is not marked as nounwind (can throw an exception),
                    // it is required to specify an unwind label to handle exceptions properly.
                    // Runtime C++ function can be marked as non-throwing using `RUNTIME_NOTHROW`.
                    val functionName = llvmFunction.name
                    val message =
                            "no exception handler specified when calling function $functionName without nounwind attr"
                    throw IllegalArgumentException(message)
                }
            }

            val success = basicBlock("call_success", position())
            val result = LLVMBuildInvoke(builder, llvmFunction, rargs, args.size, success, unwind, "")!!
            positionAtEnd(success)
            return result
        }
    }

    //-------------------------------------------------------------------------//

    fun phi(type: LLVMTypeRef, name: String = ""): LLVMValueRef {
        return LLVMBuildPhi(builder, type, name)!!
    }

    fun addPhiIncoming(phi: LLVMValueRef, vararg incoming: Pair<LLVMBasicBlockRef, LLVMValueRef>) {
        memScoped {
            val incomingValues = incoming.map { it.second }.toCValues()
            val incomingBlocks = incoming.map { it.first }.toCValues()

            LLVMAddIncoming(phi, incomingValues, incomingBlocks, incoming.size)
        }
    }

    fun assignPhis(vararg phiToValue: Pair<LLVMValueRef, LLVMValueRef>) {
        phiToValue.forEach {
            addPhiIncoming(it.first, currentBlock to it.second)
        }
    }

    fun allocInstance(typeInfo: LLVMValueRef, lifetime: Lifetime): LLVMValueRef {
        return call(context.llvm.allocInstanceFunction, listOf(typeInfo), lifetime)
    }

    fun allocInstance(descriptor: ClassDescriptor, lifetime: Lifetime): LLVMValueRef =
            allocInstance(codegen.typeInfoForAllocation(descriptor), lifetime)

    fun allocArray(
            typeInfo: LLVMValueRef, count: LLVMValueRef, lifetime: Lifetime): LLVMValueRef {
        return call(context.llvm.allocArrayFunction, listOf(typeInfo, count), lifetime)
    }

    fun unreachable(): LLVMValueRef? {
        val res = LLVMBuildUnreachable(builder)
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun br(bbLabel: LLVMBasicBlockRef): LLVMValueRef {
        val res = LLVMBuildBr(builder, bbLabel)!!
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun condBr(condition: LLVMValueRef?, bbTrue: LLVMBasicBlockRef?, bbFalse: LLVMBasicBlockRef?): LLVMValueRef? {
        val res = LLVMBuildCondBr(builder, condition, bbTrue, bbFalse)
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun blockAddress(bbLabel: LLVMBasicBlockRef): LLVMValueRef {
        return LLVMBlockAddress(function, bbLabel)!!
    }

    fun not(arg: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildNot(builder, arg, name)!!
    fun and(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildAnd(builder, arg0, arg1, name)!!
    fun or(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildOr(builder, arg0, arg1, name)!!
    fun xor(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildXor(builder, arg0, arg1, name)!!

    fun zext(arg: LLVMValueRef, type: LLVMTypeRef): LLVMValueRef =
            LLVMBuildZExt(builder, arg, type, "")!!

    fun sext(arg: LLVMValueRef, type: LLVMTypeRef): LLVMValueRef =
            LLVMBuildSExt(builder, arg, type, "")!!

    fun ext(arg: LLVMValueRef, type: LLVMTypeRef, signed: Boolean): LLVMValueRef =
            if (signed) {
                sext(arg, type)
            } else {
                zext(arg, type)
            }

    fun trunc(arg: LLVMValueRef, type: LLVMTypeRef): LLVMValueRef =
            LLVMBuildTrunc(builder, arg, type, "")!!

    private fun shift(op: LLVMOpcode, arg: LLVMValueRef, amount: Int) =
            if (amount == 0) {
                arg
            } else {
                LLVMBuildBinOp(builder, op, arg, LLVMConstInt(arg.type, amount.toLong(), 0), "")!!
            }

    fun shl(arg: LLVMValueRef, amount: Int) = shift(LLVMOpcode.LLVMShl, arg, amount)

    fun shr(arg: LLVMValueRef, amount: Int, signed: Boolean) =
            shift(if (signed) LLVMOpcode.LLVMAShr else LLVMOpcode.LLVMLShr,
                    arg, amount)

    /* integers comparisons */
    fun icmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntEQ, arg0, arg1, name)!!

    fun icmpGt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGT, arg0, arg1, name)!!
    fun icmpGe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGE, arg0, arg1, name)!!
    fun icmpLt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLT, arg0, arg1, name)!!
    fun icmpLe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLE, arg0, arg1, name)!!
    fun icmpNe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntNE, arg0, arg1, name)!!
    fun icmpUGt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntUGT, arg0, arg1, name)!!

    /* floating-point comparisons */
    fun fcmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOEQ, arg0, arg1, name)!!
    fun fcmpGt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOGT, arg0, arg1, name)!!
    fun fcmpGe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOGE, arg0, arg1, name)!!
    fun fcmpLt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOLT, arg0, arg1, name)!!
    fun fcmpLe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOLE, arg0, arg1, name)!!

    fun bitcast(type: LLVMTypeRef?, value: LLVMValueRef, name: String = "") = LLVMBuildBitCast(builder, value, type, name)!!

    fun intToPtr(value: LLVMValueRef?, DestTy: LLVMTypeRef, Name: String = "") = LLVMBuildIntToPtr(builder, value, DestTy, Name)!!
    fun ptrToInt(value: LLVMValueRef?, DestTy: LLVMTypeRef, Name: String = "") = LLVMBuildPtrToInt(builder, value, DestTy, Name)!!
    fun gep(base: LLVMValueRef, index: LLVMValueRef, name: String = ""): LLVMValueRef {
        return LLVMBuildGEP(builder, base, cValuesOf(index), 1, name)!!
    }
    fun structGep(base: LLVMValueRef, index: Int, name: String = ""): LLVMValueRef =
            LLVMBuildStructGEP(builder, base, index, name)!!

    fun extractValue(aggregate: LLVMValueRef, index: Int, name: String = ""): LLVMValueRef =
            LLVMBuildExtractValue(builder, aggregate, index, name)!!

    fun gxxLandingpad(numClauses: Int, name: String = ""): LLVMValueRef {
        val personalityFunction = LLVMConstBitCast(context.llvm.gxxPersonalityFunction, int8TypePtr)

        // Type of `landingpad` instruction result (depends on personality function):
        val landingpadType = structType(int8TypePtr, int32Type)

        return LLVMBuildLandingPad(builder, landingpadType, personalityFunction, numClauses, name)!!
    }

    fun kotlinExceptionHandler(
            block: FunctionGenerationContext.(exception: LLVMValueRef) -> Unit
    ): ExceptionHandler {
        val lpBlock = basicBlock("kotlinExceptionHandler", null)

        appendingTo(lpBlock) {
            val exception = catchKotlinException()
            block(exception)
        }

        return object : ExceptionHandler.Local() {
            override val unwind: LLVMBasicBlockRef get() = lpBlock
        }
    }

    fun catchKotlinException(): LLVMValueRef {
        val landingpadResult = gxxLandingpad(numClauses = 1, name = "lp")

        LLVMAddClause(landingpadResult, LLVMConstNull(kInt8Ptr))

        // FIXME: properly handle C++ exceptions: currently C++ exception can be thrown out from try-finally
        // bypassing the finally block.

        val exceptionRecord = extractValue(landingpadResult, 0, "er")

        // __cxa_begin_catch returns pointer to C++ exception object.
        val beginCatch = context.llvm.cxaBeginCatchFunction
        val exceptionRawPtr = call(beginCatch, listOf(exceptionRecord))

        // Pointer to KotlinException instance:
        val exceptionPtrPtr = bitcast(codegen.kObjHeaderPtrPtr, exceptionRawPtr, "")

        // Pointer to Kotlin exception object:
        // We do need a slot here, as otherwise exception instance could be freed by _cxa_end_catch.
        val exceptionPtr = loadSlot(exceptionPtrPtr, true, "exception")

        // __cxa_end_catch performs some C++ cleanup, including calling `KotlinException` class destructor.
        val endCatch = context.llvm.cxaEndCatchFunction
        call(endCatch, listOf())

        return exceptionPtr
    }

    inline fun ifThenElse(
            condition: LLVMValueRef,
            thenValue: LLVMValueRef,
            elseBlock: () -> LLVMValueRef
    ): LLVMValueRef {
        val resultType = thenValue.type

        val bbExit = basicBlock(locationInfo = position())
        val resultPhi = appendingTo(bbExit) {
            phi(resultType)
        }

        val bbElse = basicBlock(locationInfo = position())

        condBr(condition, bbExit, bbElse)
        assignPhis(resultPhi to thenValue)

        appendingTo(bbElse) {
            val elseValue = elseBlock()
            br(bbExit)
            assignPhis(resultPhi to elseValue)
        }

        positionAtEnd(bbExit)
        return resultPhi
    }

    inline fun ifThen(condition: LLVMValueRef, thenBlock: () -> Unit) {
        val bbExit = basicBlock(locationInfo = position())
        val bbThen = basicBlock(locationInfo = position())

        condBr(condition, bbThen, bbExit)

        appendingTo(bbThen) {
            thenBlock()
            if (!isAfterTerminator()) br(bbExit)
        }

        positionAtEnd(bbExit)
    }

    internal fun debugLocation(locationInfo: LocationInfo): DILocationRef? {
        if (!context.shouldContainDebugInfo()) return null
        update(currentBlock, locationInfo)
        val debugLocation = codegen.generateLocationInfo(locationInfo)
        currentPositionHolder.setBuilderDebugLocation(debugLocation)
        return debugLocation
    }

    fun indirectBr(address: LLVMValueRef, destinations: Collection<LLVMBasicBlockRef>): LLVMValueRef? {
        val indirectBr = LLVMBuildIndirectBr(builder, address, destinations.size)
        destinations.forEach { LLVMAddDestination(indirectBr, it) }
        currentPositionHolder.setAfterTerminator()
        return indirectBr
    }

    fun switch(value: LLVMValueRef, cases: Collection<Pair<LLVMValueRef, LLVMBasicBlockRef>>, elseBB: LLVMBasicBlockRef): LLVMValueRef? {
        val switch = LLVMBuildSwitch(builder, value, elseBB, cases.size)
        cases.forEach { LLVMAddCase(switch, it.first, it.second) }
        currentPositionHolder.setAfterTerminator()
        return switch
    }

    fun lookupVirtualImpl(receiver: LLVMValueRef, descriptor: FunctionDescriptor): LLVMValueRef {
        assert(LLVMTypeOf(receiver) == codegen.kObjHeaderPtr)

        val owner = descriptor.containingDeclaration as ClassDescriptor

        val typeInfoPtr: LLVMValueRef = if (descriptor.getObjCMethodInfo() != null) {
            call(context.llvm.getObjCKotlinTypeInfo, listOf(receiver))
        } else {
            val typeInfoOrMetaPtr = structGep(receiver, 0  /* typeInfoOrMeta_ */)
            val typeInfoOrMeta = load(typeInfoOrMetaPtr)
            val typeInfoPtrPtr = structGep(typeInfoOrMeta, 0 /* typeInfo */)
            load(typeInfoPtrPtr)
        }

        assert (typeInfoPtr.type == codegen.kTypeInfoPtr) { LLVMPrintTypeToString(typeInfoPtr.type)!!.toKString() }
        val llvmMethod = if (!owner.isInterface) {
            // If this is a virtual method of the class - we can call via vtable.
            val index = context.getVtableBuilder(owner).vtableIndex(descriptor as SimpleFunctionDescriptor)

            val vtablePlace = gep(typeInfoPtr, Int32(1).llvm) // typeInfoPtr + 1
            val vtable = bitcast(kInt8PtrPtr, vtablePlace)

            val slot = gep(vtable, Int32(index).llvm)
            load(slot)
        } else {
            // Otherwise, call by hash.
            // TODO: optimize by storing interface number in lower bits of 'this' pointer
            //       when passing object as an interface. This way we can use those bits as index
            //       for an additional per-interface vtable.
            val methodHash = codegen.functionHash(descriptor)                       // Calculate hash of the method to be invoked
            val lookupArgs = listOf(typeInfoPtr, methodHash)                        // Prepare args for lookup
            call(context.llvm.lookupOpenMethodFunction, lookupArgs)
        }
        val functionPtrType = pointerType(codegen.getLlvmFunctionType(descriptor))   // Construct type of the method to be invoked
        return bitcast(functionPtrType, llvmMethod)           // Cast method address to the type
    }

    fun getObjectValue(
            descriptor: ClassDescriptor,
            shared: Boolean,
            exceptionHandler: ExceptionHandler,
            locationInfo: LocationInfo?
    ): LLVMValueRef {
        if (descriptor.isUnit()) {
            return codegen.theUnitInstanceRef.llvm
        }

        if (descriptor.isCompanion) {
            val parent = descriptor.parent as IrClass
            if (parent.isObjCClass()) {
                // TODO: cache it too.

                return call(
                        codegen.llvmFunction(context.ir.symbols.interopInterpretObjCPointer.owner),
                        listOf(getObjCClass(parent, exceptionHandler)),
                        Lifetime.GLOBAL,
                        exceptionHandler
                )
            }
        }

        val objectPtr = codegen.getObjectInstanceStorage(descriptor, shared)
        val bbCurrent = currentBlock
        val bbInit= basicBlock("label_init", locationInfo)
        val bbExit= basicBlock("label_continue", locationInfo)
        val objectVal = loadSlot(objectPtr, false)
        val objectInitialized= icmpUGt(ptrToInt(objectVal, codegen.intPtrType), codegen.immOneIntPtrType)
        condBr(objectInitialized, bbExit, bbInit)

        positionAtEnd(bbInit)
        val typeInfo = codegen.typeInfoForAllocation(descriptor)
        val defaultConstructor = descriptor.constructors.first { it.valueParameters.size == 0 }
        val ctor = codegen.llvmFunction(defaultConstructor)
        val (initFunction, args) =
                if (shared) {
                    val shadowObjectPtr = codegen.getObjectInstanceShadowStorage(descriptor)
                    context.llvm.initSharedInstanceFunction to listOf(objectPtr, shadowObjectPtr, typeInfo, ctor)
                } else {
                    context.llvm.initInstanceFunction to listOf(objectPtr, typeInfo, ctor)
                }
        val newValue = call(initFunction, args, Lifetime.GLOBAL, exceptionHandler)
        val bbInitResult = currentBlock
        br(bbExit)

        positionAtEnd(bbExit)
        val valuePhi = phi(codegen.getLLVMType(descriptor.defaultType))
        addPhiIncoming(valuePhi, bbCurrent to objectVal, bbInitResult to newValue)

        return valuePhi
    }

    /**
     * Note: the same code is generated as IR in [org.jetbrains.kotlin.backend.konan.lower.EnumUsageLowering].
     */
    fun getEnumEntry(descriptor: IrEnumEntry, exceptionHandler: ExceptionHandler): LLVMValueRef {
        val enumClassDescriptor = descriptor.containingDeclaration as ClassDescriptor
        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClassDescriptor.descriptor)

        val ordinal = loweredEnum.entriesMap[descriptor.name]!!
        val values = call(
                loweredEnum.valuesGetter.llvmFunction,
                emptyList(),
                Lifetime.ARGUMENT,
                exceptionHandler
        )

        return call(
                loweredEnum.itemGetterSymbol.owner.llvmFunction,
                listOf(values, Int32(ordinal).llvm),
                Lifetime.GLOBAL,
                exceptionHandler
        )
    }

    // TODO: get rid of exceptionHandler argument by ensuring that all called functions are non-throwing.
    fun getObjCClass(irClass: IrClass, exceptionHandler: ExceptionHandler): LLVMValueRef {
        assert(!irClass.isInterface)

        return if (irClass.isExternalObjCClass()) {
            context.llvm.imports.add(irClass.llvmSymbolOrigin)

            if (irClass.isObjCMetaClass()) {
                val name = irClass.name.asString().removeSuffix("Meta")

                val objCClass = load(codegen.objCDataGenerator!!.genClassRef(name).llvm)

                val getClass = context.llvm.externalFunction(
                        "object_getClass",
                        functionType(int8TypePtr, false, int8TypePtr),
                        origin = context.standardLlvmSymbolsOrigin
                )

                call(getClass, listOf(objCClass), exceptionHandler = exceptionHandler)
            } else {
                load(codegen.objCDataGenerator!!.genClassRef(irClass.name.asString()).llvm)
            }
        } else {
            if (irClass.isObjCMetaClass()) {
                error("type-checking against Kotlin classes inheriting Objective-C meta-classes isn't supported yet")
            }

            val objCDeclarations = context.llvmDeclarations.forClass(irClass).objCDeclarations!!
            val classPointerGlobal = objCDeclarations.classPointerGlobal.llvmGlobal

            val storedClass = this.load(classPointerGlobal)

            val storedClassIsNotNull = this.icmpNe(storedClass, kNullInt8Ptr)

            return this.ifThenElse(storedClassIsNotNull, storedClass) {
                val newClass = call(
                        context.llvm.createKotlinObjCClass,
                        listOf(objCDeclarations.classInfoGlobal.llvmGlobal),
                        exceptionHandler = exceptionHandler
                )

                this.store(newClass, classPointerGlobal)
                newClass
            }
        }
    }

    fun resetDebugLocation() {
        if (!context.shouldContainDebugInfo()) return
        currentPositionHolder.resetBuilderDebugLocation()
    }

    private fun position() = basicBlockToLastLocation[currentBlock]


    internal fun mapParameterForDebug(index: Int, value: LLVMValueRef) {
        appendingTo(localsInitBb) {
            LLVMBuildStore(builder, value, vars.addressOf(index))
        }
    }

    internal fun prologue() {
        assert(returns.isEmpty())
        if (isObjectType(returnType!!)) {
            returnSlot = LLVMGetParam(function, numParameters(function.type) - 1)
        }
        positionAtEnd(localsInitBb)
        slotsPhi = phi(kObjHeaderPtrPtr)
        // Is removed by DCE trivially, if not needed.
        arenaSlot = intToPtr(
                or(ptrToInt(slotsPhi, codegen.intPtrType), codegen.immOneIntPtrType), kObjHeaderPtrPtr)
        positionAtEnd(entryBb)
    }

    internal fun epilogue() {
        appendingTo(prologueBb) {
            val slots = if (needSlots)
                LLVMBuildArrayAlloca(builder, kObjHeaderPtr, Int32(slotCount).llvm, "")!!
            else
                kNullObjHeaderPtrPtr
            if (needSlots) {
                // Zero-init slots.
                val slotsMem = bitcast(kInt8Ptr, slots)
                call(context.llvm.memsetFunction,
                        listOf(slotsMem, Int8(0).llvm,
                                Int32(slotCount * codegen.runtime.pointerSize).llvm,
                                Int32(codegen.runtime.pointerAlignment).llvm,
                                Int1(0).llvm))
                call(context.llvm.enterFrameFunction, listOf(slots, Int32(vars.skip).llvm, Int32(slotCount).llvm))
            }
            addPhiIncoming(slotsPhi!!, prologueBb to slots)
            memScoped {
                slotToVariableLocation.forEach { slot, variable ->
                    val expr = longArrayOf(DwarfOp.DW_OP_plus_uconst.value,
                            runtime.pointerSize * slot.toLong()).toCValues()
                    DIInsertDeclaration(
                            builder       = codegen.context.debugInfo.builder,
                            value         = slots,
                            localVariable = variable.localVariable,
                            location      = variable.location,
                            bb            = prologueBb,
                            expr          = expr,
                            exprCount     = 2)
                }
            }
            br(localsInitBb)
        }

        appendingTo(localsInitBb) {
            br(entryBb)
        }

        appendingTo(epilogueBb) {
            when {
                returnType == voidType -> {
                    releaseVars()
                    assert(returnSlot == null)
                    LLVMBuildRetVoid(builder)
                }
                returns.isNotEmpty() -> {
                    val returnPhi = phi(returnType!!)
                    addPhiIncoming(returnPhi, *returns.toList().toTypedArray())
                    if (returnSlot != null) {
                        updateReturnRef(returnPhi, returnSlot!!)
                    }
                    releaseVars()
                    LLVMBuildRet(builder, returnPhi)
                }
            // Do nothing, all paths throw.
                else -> LLVMBuildUnreachable(builder)
            }
        }

        appendingTo(cleanupLandingpad) {
            val landingpad = gxxLandingpad(numClauses = 0)
            LLVMSetCleanup(landingpad, 1)

            forwardingForeignExceptionsTerminatedWith?.let { terminator ->
                val kotlinExceptionRtti = constPointer(importGlobal(
                        "_ZTI9ObjHolder", // typeinfo for ObjHolder
                        int8TypePtr,
                        origin = context.stdlibModule.llvmSymbolOrigin
                ))

                // Catch all but Kotlin exceptions.
                val clause = ConstArray(int8TypePtr, listOf(kotlinExceptionRtti.bitcast(int8TypePtr)))
                LLVMAddClause(landingpad, clause.llvm)

                val bbCleanup = basicBlock("forwardException", null)
                val bbUnexpected = basicBlock("unexpectedException", null)

                val selector = extractValue(landingpad, 1)
                condBr(
                        icmpLt(selector, Int32(0).llvm),
                        bbUnexpected,
                        bbCleanup
                )

                appendingTo(bbUnexpected) {
                    val exceptionRecord = extractValue(landingpad, 0)

                    val beginCatch = context.llvm.cxaBeginCatchFunction
                    // So `terminator` is called from C++ catch block:
                    call(beginCatch, listOf(exceptionRecord))
                    call(terminator, emptyList())
                    unreachable()
                }

                positionAtEnd(bbCleanup)
            }

            releaseVars()
            LLVMBuildResume(builder, landingpad)
        }

        returns.clear()
        vars.clear()
        returnSlot = null
        slotsPhi = null
    }

    //-------------------------------------------------------------------------//

    /**
     * Represents the mutable position of instructions being inserted.
     *
     * This class is introduced to workaround unreachable code handling.
     */
    inner class PositionHolder {
        private val builder: LLVMBuilderRef = LLVMCreateBuilder()!!


        fun getBuilder(): LLVMBuilderRef {
            if (isAfterTerminator) {
                positionAtEnd(basicBlock("unreachable", null))
            }

            return builder
        }

        /**
         * Should be `true` iff the position is located after terminator instruction.
         */
        var isAfterTerminator: Boolean = false
            private set

        fun setAfterTerminator() {
            isAfterTerminator = true
        }

        val currentBlock: LLVMBasicBlockRef
            get() = LLVMGetInsertBlock(builder)!!

        fun positionAtEnd(block: LLVMBasicBlockRef) {
            LLVMPositionBuilderAtEnd(builder, block)
            basicBlockToLastLocation[block]?.let{ debugLocation(it) }
            val lastInstr = LLVMGetLastInstruction(block)
            isAfterTerminator = lastInstr != null && (LLVMIsATerminatorInst(lastInstr) != null)
        }

        fun dispose() {
            LLVMDisposeBuilder(builder)
        }

        fun resetBuilderDebugLocation() {
            if (!context.shouldContainDebugInfo()) return
            LLVMBuilderResetDebugLocation(builder)
        }

        fun setBuilderDebugLocation(debugLocation: DILocationRef?) {
            if (!context.shouldContainDebugInfo()) return
            LLVMBuilderSetDebugLocation(builder, debugLocation)
        }
    }

    private var currentPositionHolder: PositionHolder = PositionHolder()

    /**
     * Returns `true` iff the current code generation position is located after terminator instruction.
     */
    fun isAfterTerminator() = currentPositionHolder.isAfterTerminator

    val currentBlock: LLVMBasicBlockRef
        get() = currentPositionHolder.currentBlock

    /**
     * The builder representing the current code generation position.
     *
     * Note that it shouldn't be positioned directly using LLVM API due to some hacks.
     * Use e.g. [positionAtEnd] instead. See [PositionHolder] for details.
     */
    val builder: LLVMBuilderRef
        get() = currentPositionHolder.getBuilder()

    fun positionAtEnd(bbLabel: LLVMBasicBlockRef) = currentPositionHolder.positionAtEnd(bbLabel)

    inline private fun <R> preservingPosition(code: () -> R): R {
        val oldPositionHolder = currentPositionHolder
        val newPositionHolder = PositionHolder()
        currentPositionHolder = newPositionHolder
        try {
            return code()
        } finally {
            currentPositionHolder = oldPositionHolder
            newPositionHolder.dispose()
        }
    }

    inline fun <R> appendingTo(block: LLVMBasicBlockRef, code: FunctionGenerationContext.() -> R) = preservingPosition {
        positionAtEnd(block)
        code()
    }

    private val needSlots: Boolean
        get() {
            return slotCount > frameOverlaySlotCount || localAllocs > 0
        }

    private fun releaseVars() {
        if (needSlots) {
            call(context.llvm.leaveFrameFunction,
                    listOf(slotsPhi!!, Int32(vars.skip).llvm, Int32(slotCount).llvm))
        }
    }
}



