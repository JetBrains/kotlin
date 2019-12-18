// "Change to 'return@foo'" "true"
inline fun foo(f: (Int) -> Int?) {}

fun baz(): Int = 0

fun test() {
    foo { i ->
        if (i == 1) return null<caret>
        0
    }
}