interface A
class B: A

val X = B()

fun foo(b: B) {}

fun main(a: A) {
    if (a === X) {
        foo(a)
    }
}