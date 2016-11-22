import kotlin_.cinterop.*
import llvm.*

fun main(args: Array<String>) = memScoped {
    val module = LLVMModuleCreateWithName("module")
    println("module=" + module.rawValue)

    val paramTypes = allocArrayOf(LLVMInt32Type(), LLVMInt32Type())
    val retType = LLVMFunctionType(LLVMInt32Type(), paramTypes[0].ptr, 2, 0)

    val sum = LLVMAddFunction(module, "sum", retType)
    val entry = LLVMAppendBasicBlock(sum, "entry")
    val builder = LLVMCreateBuilder()
    LLVMPositionBuilderAtEnd(builder, entry)
    val tmp = LLVMBuildAdd(builder, LLVMGetParam(sum, 0), LLVMGetParam(sum, 1), "tmp")
    LLVMBuildRet(builder, tmp)
    val engineRef = alloc<LLVMExecutionEngineRefVar>()
    val errorRef = allocPointerTo<CInt8Var>()
    LLVMInitializeNativeTarget()
    errorRef.value = null
    if (LLVMCreateExecutionEngineForModule(engineRef.ptr, module, errorRef.ptr) != 0) {
        println("failed to create execution engine")
        return
    }
    val error = errorRef.value
    if (error != null) {
        println(error.asCString().toString())
        return
    }

    println(LLVMGetTypeKind(LLVMInt32Type()))
    val x = 5L
    val y = 6L
    val args = allocArrayOf(
            LLVMCreateGenericValueOfInt(LLVMInt32Type(), x, 0),
            LLVMCreateGenericValueOfInt(LLVMInt32Type(), y, 0))

    val runRes = LLVMRunFunction(engineRef.value, sum, 2, args[0].ptr)
    println(LLVMGenericValueToInt(runRes, 0))
    if (LLVMWriteBitcodeToFile(module, "/tmp/sum.bc") != 0) {
        println("error writing bitcode to file, skipping")
    }
    LLVMDisposeBuilder(builder)
    LLVMDisposeExecutionEngine(engineRef.value)
}