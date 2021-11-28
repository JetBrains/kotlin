// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// WITH_STDLIB

class Host {
    val ok = "OK"

    fun foo() = run { bar(ok) }

    companion object {
        val ok = 0

        fun bar(s: String) = s.substring(ok)
    }
}

fun box() = Host().foo()