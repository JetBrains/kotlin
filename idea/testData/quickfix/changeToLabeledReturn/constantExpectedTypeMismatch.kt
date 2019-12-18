// "Change to 'return@foo'" "true"
inline fun foo(f: (Int) -> Int) {}

fun test() {
    foo { i ->
        if (i == 1) return 1<caret>
        0
    }
}