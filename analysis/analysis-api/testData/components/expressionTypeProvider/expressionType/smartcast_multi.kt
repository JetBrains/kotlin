interface A
interface B

fun <T> T.take() where T: A, T: B {}

fun test(a: Any) {
    if (a is A && a is B) {
        <expr>a</expr>.take()
    }
}
