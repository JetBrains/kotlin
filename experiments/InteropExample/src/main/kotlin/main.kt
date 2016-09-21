import kotlin_native.interop.*
import llvm.*

fun main(args: Array<String>) {
    val module = LLVMModuleCreateWithName("module")
    println("module=" + module.getNativePtr().asLong())

    val paramTypes = mallocNativeArrayOf(LLVMOpaqueType, LLVMInt32Type(), LLVMInt32Type())
    val retType = LLVMFunctionType(LLVMInt32Type(), paramTypes[0], 2, 0)
    free(paramTypes)

    val sum = LLVMAddFunction(module, "sum", retType)
    val entry = LLVMAppendBasicBlock(sum, "entry")
    val builder = LLVMCreateBuilder()
    LLVMPositionBuilderAtEnd(builder, entry)
    val tmp = LLVMBuildAdd(builder, LLVMGetParam(sum, 0), LLVMGetParam(sum, 1), "tmp")
    LLVMBuildRet(builder, tmp)
    val engineRef = malloc(Ref to LLVMOpaqueExecutionEngine)
    val errorRef = malloc(Ref to Int8Box)
    LLVMInitializeNativeTarget()
    errorRef.value = null
    if (LLVMCreateExecutionEngineForModule(engineRef, module, errorRef) != 0) {
        println("failed to create execution engine")
        return
    }
    val error = errorRef.value
    if (error != null) {
        println(CString.fromArray(NativeArray.byRefToFirstElem(error, Int8Box)).toString())
        return
    }

    println(LLVMGetTypeKind(LLVMInt32Type()))
    val x = 5L
    val y = 6L
    val args = malloc(array[2](Ref to LLVMOpaqueGenericValue))
    args[0].value = LLVMCreateGenericValueOfInt(LLVMInt32Type(), x, 0)
    args[1].value = LLVMCreateGenericValueOfInt(LLVMInt32Type(), y, 0)
    val runRes = LLVMRunFunction(engineRef.value, sum, 2, args[0])
    println(LLVMGenericValueToInt(runRes, 0))
    if (LLVMWriteBitcodeToFile(module, "/tmp/sum.bc") != 0) {
        println("error writing bitcode to file, skipping")
    }
    LLVMDisposeBuilder(builder)
    LLVMDisposeExecutionEngine(engineRef.value)



}