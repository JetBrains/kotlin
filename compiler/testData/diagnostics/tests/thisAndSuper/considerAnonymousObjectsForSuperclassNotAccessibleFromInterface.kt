// RUN_PIPELINE_TILL: BACKEND
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

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, interfaceDeclaration, override,
superExpression */
