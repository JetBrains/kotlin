class A
class B
class C

data class Args(val a: A, val c: C)

fun foo(a: A, b: B, c: C) {}

fun bar(args: Args, b: B) {
    <caret>foo(...args, b = b)
}
