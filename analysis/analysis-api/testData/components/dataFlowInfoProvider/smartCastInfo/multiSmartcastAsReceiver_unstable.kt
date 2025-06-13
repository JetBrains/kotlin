interface A
interface B

fun <T> T.foo() where T: A, T: B {}

var a: Any = 1
fun test() {
    if (a is A && a is B) {
        <expr>a</expr>.foo()
    }
}