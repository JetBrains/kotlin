// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74459
// LANGUAGE: -ForbidUsingExpressionTypesWithInaccessibleContent -ForbidUsingSupertypesWithInaccessibleContentInTypeArguments -ForbidLambdaParameterWithMissingDependencyType -AllowEagerSupertypeAccessibilityChecks
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
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>Short<!>(s) { null }
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>Short<!>(s) { <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>Derived<!>() }

    Impl(s)
    <!MISSING_DEPENDENCY_CLASS!>Impl<!>(s, null)
    <!MISSING_DEPENDENCY_CLASS!>Impl<!>(s, <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>Derived<!>())
    <!MISSING_DEPENDENCY_CLASS!>Impl<!>(s, null) { null }
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>Impl<!>(s, f = { null })
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>Impl<!>(s, f = { <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>Derived<!>() })

    impl(s)
    <!MISSING_DEPENDENCY_CLASS!>impl<!>(s, null)
    <!MISSING_DEPENDENCY_CLASS!>impl<!>(s, <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>Derived<!>())
    <!MISSING_DEPENDENCY_CLASS!>impl<!>(s, null) { null }
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>impl<!>(s, f = { null })
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>impl<!>(s, f = { <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>Derived<!>() })

    Another(s)
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>Another<!>(s) <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>{ false }<!>
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>Another<!>(s) <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>{ <!MISSING_DEPENDENCY_CLASS!>it<!> == <!MISSING_DEPENDENCY_CLASS!>it<!> }<!>
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>Another<!>(s) <!MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER!>{ <!MISSING_DEPENDENCY_CLASS!>it<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>() == 0 }<!>
}
