// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74459
// LANGUAGE: -ForbidUsingExpressionTypesWithInaccessibleContent -ForbidUsingSupertypesWithInaccessibleContentInTypeArguments -ForbidLambdaParameterWithMissingDependencyType
// MODULE: base
// FILE: base.kt

interface Base

// MODULE: intermediate(base)
// FILE: intermediate.kt

class Derived : Base
class Short(val s: String, val f: () -> Base? = { null })
class Impl(val s: String, val d: Base? = null, val f: () -> Base? = { null })
fun impl(s: String, d: Base? = null, f: () -> Base? = { null }) {}
class Another(val s: String, val f: (Base) -> Boolean = { true })

// MODULE: use(intermediate)
// FILE: use.kt

fun foo(s: String) {
    Short(s)
    Short(s) { null }
    Short(s) { Derived() }

    Impl(s)
    Impl(s, null)
    Impl(s, Derived())
    Impl(s, null) { null }
    Impl(s, f = { null })
    Impl(s, f = { Derived() })

    impl(s)
    impl(s, null)
    impl(s, Derived())
    impl(s, null) { null }
    impl(s, f = { null })
    impl(s, f = { Derived() })

    Another(s)
    Another(s) { false }
    Another(s) { it == it }
    Another(s) { it.hashCode() == 0 }
}
