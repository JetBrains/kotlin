class A
class B

data class Args(val a: A, val b: B)

fun foo(a: A, b: B) {}

fun bar(args: Args) {
    <caret>foo(...args)
}
