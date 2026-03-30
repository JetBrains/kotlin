class A
class B
class C

data class Left(val a: A, val b: B)
data class Right(val b: B, val c: C)

fun foo(a: A, b: B, c: C) {}

fun bar(left: Left, right: Right) {
    <caret>foo(...left, ...right)
}
