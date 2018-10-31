// !DIAGNOSTICS: -NOTHING_TO_INLINE
fun main() {
    test {
        <!RETURN_NOT_ALLOWED!>return<!>
    }
}

inline fun test(noinline lambda: () -> Unit) {
    lambda()
}