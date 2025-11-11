// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76240
// IDE_MODE

fun Int.f(): String = "ext func"
val Int.p: String
    get() = "ext prop"

class Foo {
    val f = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>f()<!>
    fun f() = 42.<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>f<!>()

    val p = p()
    fun p() = 42.p
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
