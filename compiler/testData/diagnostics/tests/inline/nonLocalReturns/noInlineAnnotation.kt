// !DIAGNOSTICS: -NOTHING_TO_INLINE
fun main(args: Array<String>) {
    test {
        <!RETURN_NOT_ALLOWED!>return<!>
    }
}

inline fun test(noinline lambda: () -> Unit) {
    lambda()
}