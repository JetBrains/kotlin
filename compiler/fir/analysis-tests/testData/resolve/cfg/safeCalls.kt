// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG
interface A {
    fun foo(): A
    fun bar(x: String): A
}

interface B {
    val foo: B
    val bar: B
}

fun test_1(x: A?) {
    x?.foo()?.bar("")
}

fun test_2(x: B?) {
    x?.foo?.bar
}

fun test_3(x: A?, y: String?) {
    if (x?.bar(y as String) != null) {
        y.length
    }
}
