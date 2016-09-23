package org.kotlinnative.translator.llvm.bc

import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMType
import java.util.*

/**
 * Created by minamoto on 23/09/2016.
 */
class BcLlvmBuilder {
    fun comment(comment: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    fun heapAndCall(target: LLVMVariable, asValue: Boolean, pointer: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun globalInitialize(target: LLVMVariable, fields: ArrayList<LLVMClassVariable>, initializers: Map<LLVMVariable, String>, classType: LLVMType) {
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


}