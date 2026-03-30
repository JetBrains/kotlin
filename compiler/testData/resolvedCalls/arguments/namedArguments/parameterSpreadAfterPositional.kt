class A
class B
class C

data class Tail(val b: B, val c: C)

fun foo(a: A, b: B, c: C) {}

fun bar(a: A, tail: Tail) {
    <caret>foo(a, ...tail)
}
