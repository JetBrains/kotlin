interface A
interface B
interface C: A, B

fun foo(i: C?) {
    val x = <expr>i</expr>
}
