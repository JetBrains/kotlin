// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ForbidUsingExpressionTypesWithInaccessibleContent +ForbidUsingSupertypesWithInaccessibleContentInTypeArguments +ForbidLambdaParameterWithMissingDependencyType -AllowEagerSupertypeAccessibilityChecks

// MODULE: missing
// FILE: Base.java
public class Base {}

// MODULE: intermediate(missing)
// FILE: Derived.java
public class Derived extends Base {}

// MODULE: use(intermediate)
// FILE: use.kt
fun foo(): Derived = Derived()

