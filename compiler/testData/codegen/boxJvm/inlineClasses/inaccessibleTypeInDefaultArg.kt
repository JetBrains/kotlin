// IGNORE_BACKEND: ANY
// ^ This test should results in compilation error. At the moment it fails with NoSuchMethodError.

// ISSUE: KT-87904
// WITH_STDLIB

// MODULE: base
// FILE: base.kt

@JvmInline
value class AnInlineClass(val value: String)

// MODULE: intermediate(base)
// FILE: intermediate.kt

class DependencyClass(val parameter: AnInlineClass = AnInlineClass("default"))

fun dependencyFunction(parameter: AnInlineClass = AnInlineClass("default")) {
}

// MODULE: use(intermediate)
// FILE: use.kt

fun box(): String {
    DependencyClass()
    dependencyFunction()
    return "OK"
}
