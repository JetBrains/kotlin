package org.kotlinnative.translator

import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.codegens.ClassCodegen
import org.kotlinnative.translator.codegens.FunctionCodegen
import org.kotlinnative.translator.codegens.ObjectCodegen
import org.kotlinnative.translator.codegens.PropertyCodegen
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import java.util.*


class TranslationState
private constructor
(
        val bindingContext: BindingContext,
        val mainFunction: String,
        arm: Boolean
) {
    var externalFunctions = HashMap<String, FunctionCodegen>()
    var functions = HashMap<String, FunctionCodegen>()
    var classes = HashMap<String, ClassCodegen>()
    var objects = HashMap<String, ObjectCodegen>()
    var properties = HashMap<String, PropertyCodegen>()
    val codeBuilder = LLVMBuilder(arm)
    val extensionFunctions = HashMap<String, HashMap<String, FunctionCodegen>>()
    val globalVariableCollection = HashMap<String, LLVMVariable>()

    init {
        POINTER_ALIGN = if (arm) 4 else 8
        POINTER_SIZE = if (arm) 4 else 8
    }

    companion object {
        var POINTER_ALIGN = 4
        var POINTER_SIZE = 4

        fun createTranslationState(bindingContext:BindingContext, mainFunction: String, arm: Boolean = false): TranslationState {
            return TranslationState(bindingContext, mainFunction, arm)
        }
    }

}