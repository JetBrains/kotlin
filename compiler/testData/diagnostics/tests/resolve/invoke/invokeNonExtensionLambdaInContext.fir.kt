// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

class C {
    fun f() {}
}

fun C.g(f: (String) -> Unit = { s -> f() }) {}
