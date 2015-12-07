// "Create function 'foo'" "true"
fun <T, U> T.map(f: (T) -> U) = f(this)

fun test() {
    1.map(::<caret>foo)
}