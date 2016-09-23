package org.kotlinnative.translator.llvm.ir

import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMStringType
import org.kotlinnative.translator.llvm.types.LLVMType
import java.util.*

/**
 * Created by minamoto on 23/09/2016.
 */
class IrLlvmBuilder {
    private val localCode: StringBuilder = StringBuilder()
    private val globalCode: StringBuilder = StringBuilder()
    private val arm = true /* architecture specific TBD */
    private val exceptions = mapOf(
            Pair("KotlinNullPointerException", initializeExceptionString("Exception in thread main kotlin.KotlinNullPointerException")))


    init {
        val declares = arrayOf(
                "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)",
                "declare i8* @malloc_heap(i32)",
                "declare i32 @printf(i8*, ...)",
                "declare void @abort()",
                "%class.Nothing = type { }")

        declares.forEach { addLLVMCodeToGlobalPlace(it) }

        if (arm) {
            val functionAttributes = """attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }"""
            addLLVMCodeToGlobalPlace(functionAttributes)
        }
    }

    fun addLLVMCodeToLocalPlace(code: String) =
            localCode.appendln(code)

    fun addLLVMCodeToGlobalPlace(code: String) =
            globalCode.appendln(code)

    private fun initializeExceptionString(string: String): LLVMVariable {
        TODO()
        /*
        val result = getNewVariable(LLVMStringType(string.length), pointer = 0, scope = LLVMVariableScope(), prefix = "exceptions.str.")
        addStringConstant(result, string)
        return result
        */
    }

    fun comment(comment: String) {
        TODO()
    }

    fun startExpression() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun endExpression() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun assignment(lhs: LLVMVariable, rhs: LLVMNode) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun returnOperation(llvmVariable: LLVMSingleValue) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun anyReturn(type: LLVMType, value: String, pointer: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun stringConstant(variable: LLVMVariable, value: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun conditionalBranch(condition: LLVMSingleValue, thenLabel: LLVMLabel, elseLabel: LLVMLabel) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun unconditionalBranch(label: LLVMLabel) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun abort(exceptionName: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun call(functionName: LLVMVariable, arguments: List<LLVMVariable>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun alloca(target: LLVMVariable, asValue: Boolean, pointer: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun heapAndCast(target: LLVMVariable, asValue: Boolean, pointer: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun globalinitialize(target: LLVMVariable, fields: ArrayList<LLVMClassVariable>, initializers: Map<LLVMVariable, String>, classType: LLVMType) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun bitcast(src: LLVMVariable, llvmType: LLVMType, pointer: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun classDeclaration(name: String, fields: List<LLVMVariable>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun globalDeclaration(variable: LLVMVariable, defaultValue: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun loadClassField(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun loadVariableOffset(target: LLVMVariable, source: LLVMVariable, index: LLVMConstant) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun load(source: LLVMVariable) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun label(label: LLVMLabel) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun structInitializer(args: List<LLVMVariable>, values: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun memcpy(castedDst: LLVMVariable, castedSrc: LLVMVariable, size: Int, align: Int, volatile: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun storeString(target: LLVMVariable, source: LLVMVariable, offset: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun storeVariable(target: LLVMSingleValue, source: LLVMSingleValue) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun storeNull(result: LLVMVariable) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}