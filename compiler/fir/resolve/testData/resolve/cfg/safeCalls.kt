// !DUMP_CFG
interface A {
    fun foo(): A
    fun bar(): A
}

interface B {
    val foo: B
    val bar: B
}

fun test_1(x: A?) {
    x?.foo()?.bar()
}

fun test_2(x: B?) {
    x?.foo?.bar
}
