// "Create class 'Foo'" "true"
// ERROR: Unresolved reference: Foo

class A(val n: Int)

fun test() = J.Foo(abc = 1, ghi = A(2), def = "s")