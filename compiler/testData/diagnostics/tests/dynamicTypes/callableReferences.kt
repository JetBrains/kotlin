// !DIAGNOSTICS: -REFLECTION_TYPES_NOT_LOADED -UNUSED_EXPRESSION

// MODULE[js]: m1
// FILE: k.kt

fun test() {
    dynamic::foo
}

class dynamic {
    fun foo() {}
}