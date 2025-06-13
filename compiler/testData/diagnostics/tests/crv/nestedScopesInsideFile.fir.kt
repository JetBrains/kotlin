// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun localFun() {
    fun local(): Int = 123
    <!RETURN_VALUE_NOT_USED!>local()<!>     //unused
}

class A {
    fun foo(): Int = 123
    fun test() {
        <!RETURN_VALUE_NOT_USED!>foo()<!>               //unused
    }
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, functionDeclaration, integerLiteral, localFunction */
