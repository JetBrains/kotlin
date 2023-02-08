class A
class B

typealias Foo = suspend context(A, B) String.(Int, Double) -> String

fun foo(f: Foo) {}