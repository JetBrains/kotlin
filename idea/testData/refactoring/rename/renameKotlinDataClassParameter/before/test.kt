package test

data class A(val /*rename*/foo: Int, val bar: String)

fun test(a: A) {
    a.foo
    a.component1()
    a.bar
    a.component2()
}