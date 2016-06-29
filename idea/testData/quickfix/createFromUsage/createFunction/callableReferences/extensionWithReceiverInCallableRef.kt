// "Create extension function 'Int.foo'" "true"
// WITH_RUNTIME
fun <T, U> T.map(f: T.() -> U) = f()

fun consume(s: String) {}

fun test() {
    consume(1.map(Int::<caret>foo))
}