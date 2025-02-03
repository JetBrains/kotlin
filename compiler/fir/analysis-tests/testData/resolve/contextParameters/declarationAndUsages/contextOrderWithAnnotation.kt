// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-51258
// LANGUAGE: +ContextParameters

annotation class Ann

context(a: String)
@Ann
fun foo(): String {
    return a
}

@Ann
context(a: String)
fun bar() { }

context(a: String)
@Ann
val qux : String
    get() = ""

@Ann
context(a: String)
val buz : String
    get() = ""