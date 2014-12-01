// !DIAGNOSTICS: -REFLECTION_TYPES_NOT_LOADED -UNUSED_EXPRESSION

fun test() {
    dynamic::foo
}

class dynamic {
    fun foo() {}
}