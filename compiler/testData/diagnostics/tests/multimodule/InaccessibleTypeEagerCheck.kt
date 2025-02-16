// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ForbidUsingExpressionTypesWithInaccessibleContent +ForbidUsingSupertypesWithInaccessibleContentInTypeArguments +ForbidLambdaParameterWithMissingDependencyType -AllowEagerSupertypeAccessibilityChecks

// MODULE: missing
// FILE: Base.kt
open class Base {}

// MODULE: intermediate(missing)
// FILE: Derived.kt
class Derived : Base() {}

// MODULE: use(intermediate)
// FILE: use.kt
fun foo(): Derived = Derived()

