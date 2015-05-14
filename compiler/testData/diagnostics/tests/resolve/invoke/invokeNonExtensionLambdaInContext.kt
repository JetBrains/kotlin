// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    fun f() {}
}

fun C.g(f: (String) -> Unit = { s -> f() }) {}
