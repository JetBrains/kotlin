class A
class B
class C

data class Head(val a: A)
data class Tail(val b: B, val c: C)

fun foo(a: A, b: B, c: C) {}

fun bar(head: Head, tail: Tail, b: B) {
    <caret>foo(...head, b = b, ...tail)
}
