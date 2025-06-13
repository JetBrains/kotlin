interface A
interface B

fun test(a: Any) {
    if (a is A && a is B) {
        <expr>a</expr>
    }
}