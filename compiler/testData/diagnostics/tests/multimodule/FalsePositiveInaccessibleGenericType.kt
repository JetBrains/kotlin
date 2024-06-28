// LANGUAGE: -ForbidUsingExpressionTypesWithInaccessibleContent
// ISSUE: KT-66690
// MODULE: base
// FILE: base.kt

class Generic<T>

// MODULE: intermediate(base)
// FILE: intermediate.kt

class Owner<T>

interface Some<S> {
    val g: Owner<Generic<S>>
}

fun register(owner: Owner<*>) {}

// MODULE: user(intermediate)
// FILE: user.kt

fun test(some: Some<String>) {
    register(some.g)
}

fun test2(some: Some<String>) {
    val a = some.g
    register(a)
}
