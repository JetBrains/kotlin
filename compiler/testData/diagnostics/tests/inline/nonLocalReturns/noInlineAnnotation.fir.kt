// !DIAGNOSTICS: -NOTHING_TO_INLINE
fun main() {
    test {
        return
    }
}

inline fun test(noinline lambda: () -> Unit) {
    lambda()
}