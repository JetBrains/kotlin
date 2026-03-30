class A
class B
class C
class D

data class First(val a: A, val b: B)
data class Second(val b: B, val c: C)
data class Third(val c: C, val d: D)

fun foo(a: A, b: B, c: C, d: D) {}

fun bar(first: First, second: Second, third: Third) {
    <caret>foo(...first, ...second, ...third)
}
