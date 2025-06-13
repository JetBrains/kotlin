// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
open class Aaa() {
    fun foo() = 1
}

open class Bbb() : Aaa() {
    <!CONFLICTING_OVERLOADS!>fun <T> foo()<!> = 2
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, primaryConstructor,
typeParameter */
