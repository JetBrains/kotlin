interface A
interface B

var a: Any = 1
fun test() {
    if (a is A && a is B) {
        <expr>a</expr>
    }
}