// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76240
// IDE_MODE

fun Int.f(): String = "ext func"
val Int.p: String
    get() = "ext prop"

class Foo {
    val f = <!DEBUG_INFO_LEAKING_THIS!>f<!>()
    fun f() = 42.f()

    val p = <!DEBUG_INFO_LEAKING_THIS!>p<!>()
    fun p() = 42.p
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
