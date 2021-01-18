interface A
interface B

fun foo(x: A) {}
fun foo(x: B) {}

open class C : A, B

fun main(a: A) {
    foo(a)

    val anonymousA: A = object : C() {}
    foo(anonymousA)
}
