// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidUsingExpressionTypesWithInaccessibleContent +ForbidUsingSupertypesWithInaccessibleContentInTypeArguments +ForbidLambdaParameterWithMissingDependencyType +AllowEagerSupertypeAccessibilityChecks

// MODULE: missing
// FILE: Base.kt
open class Base {}

// MODULE: intermediate(missing)
// FILE: Derived.kt
class Derived : Base() {}

// MODULE: use(intermediate)
// FILE: use.kt
fun foo(): Derived = <!MISSING_DEPENDENCY_SUPERCLASS!>Derived<!>()

