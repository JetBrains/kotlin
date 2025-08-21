// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun localFun() {
    fun local(): Int = 123
    local()     //unused
}

class A {
    fun foo(): Int = 123
    fun test() {
        foo()               //unused
    }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, functionDeclaration, integerLiteral, localFunction */
