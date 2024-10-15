// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-54673

open class KotlinBaseClass {
    open fun kotlinFun() {}
}

interface sealedInterface {
    fun someFun() {
        object : KotlinBaseClass() {
            override fun kotlinFun() {
                super.kotlinFun()
            }
        }
    }
}