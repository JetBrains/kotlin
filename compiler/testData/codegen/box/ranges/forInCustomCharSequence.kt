// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

class C : CharSequence {
    // Unused declarations, which are here only to confuse the backend who might lookup symbols by name
    private val List<String>.length: Int
        get() = size
    private operator fun List<String>.get(i: Int) =
        this.get(i)

    override val length: Int
        get() = 2

    override fun get(index: Int): Char =
        if (index == 0) 'O' else 'K'

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        throw AssertionError()
}

fun box(): String {
    var result = ""
    val c = C()
    for (i in c.indices) {
        if (i == 0) {
            result += c[i]
        }
    }
    for ((i, x) in c.withIndex()) {
        if (i == 1) {
            result += x
        }
    }
    return result
}
