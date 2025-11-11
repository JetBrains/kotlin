// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76240
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: NewWarnings.kt

fun Int.f(): String = "ext func"
val Int.p: String
    get() = "ext prop"

class Foo {
    val f = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>f()<!>
    fun f() = 42.<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>f<!>() // New warning even if `f` is declared above and seems like resolved.

    val p = p()
    fun p() = 42.p // No warning because there is no attempt to resolve to an invoke call
}

fun String.g(): Boolean = false

class Bar {
    fun g() = "s2".<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>g<!>() // New warning
    val g = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>g()<!>
}

// FILE: NoWarningsInCaseOfLocalProperty.kt

fun Int.f2(): String = "ext func"

fun test() {
    val f2 = "str"
    fun f2() = 42.f2()
}

// FILE: NoWarningsInCaseOfNullExplicitReceiver.kt

fun Int.f3(): String = "ext func"

class Foo3 {
    val f3 = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>f3()<!>
    fun f3() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>f3()<!>
}

// FILE: NoWarningsInCaseOfResolveToInvoke.kt

fun f4() = O1.x1()

object O1 {
    val x1 = O1
    operator fun invoke(): String = "s1"
}

// FILE: NoWarningsInCaseOfPropertyWithImplicitTypeThatIsInferredToAnExtensionFuncitonType.kt

fun Int.g5(): String = "g5 ext func"

class Foo5 {
    val f5: Int.() -> Unit = {}
    val g5 = f5
    val x = 42.g5() // Should be resolved to the member property with implicit extension function type
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
localFunction, localProperty, objectDeclaration, operator, propertyDeclaration, propertyWithExtensionReceiver,
stringLiteral */
