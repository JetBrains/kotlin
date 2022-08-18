// WITH_STDLIB

inline fun <reified T> baz(value: T): String = "OK" + value

fun test(): String {
    val f: (Any) -> String = ::baz
    return f(1)
}

object Foo {
    val log = "123"
}

public inline fun <reified T> Foo.foo(value: T): String =
    log + value

val test2 = { "OK".let(Foo::foo) }

object Bar {
    val log = "321"

    public inline fun <reified T> bar(value: T): String =
        log + value
}

val test3 = { "OK".let(Bar::bar) }

class C {
    inline fun <reified T: String> qux(value: T): String = "OK" + value
}

fun test4(): String {
    val c = C()
    val cr: (String) -> String = c::qux
    return cr("456")
}

inline fun <reified T: Any> ((Any) -> String).cux(value: T): String = this(value)

fun test5(): String {
    val foo: (Any) -> String = ({ b: Any ->
        val a: (Any) -> String = ::baz
        a(b)
    })::cux
    return foo(3)
}

inline fun <reified T, K, reified S> bak(value1: T, value2: K, value3: S): String = "OK" + value1 + value2 + value3

fun test6(): String {
    val f: (Any, Int, String) -> String = ::bak
    return f(1, 37, "joo")
}

inline fun <reified T, K> bal(value1: Array<K>, value2: Array<T>): String = "OK" + value1.joinToString() + value2.joinToString()

fun test7(): String {
    val f: (Array<Any>, Array<Int>) -> String = ::bal
    return f(arrayOf("mer", "nas"), arrayOf(73, 37))
}

class E<T>
public inline fun <reified T> E<T>.foo(value: T): String = "OK" + value

class F<T1> {
    inline fun <reified T2> foo(x: T1, y: T2): Any? = "OK" + x + y
}

inline fun <reified T, K> bam(value1: K?, value2: T?): String = "OK" + value1.toString() + value2.toString()

fun <T> test10(): String {
    val f: (T?, String?) -> String = ::bam
    return f(null, "abc")
}

inline fun <T> test11Impl() : String {
    val f: (T?, String?) -> String = ::bam
    return f(null, "def")
}

fun <T> test11() = test11Impl<T>()


fun box(): String {
    val test1 = test()
    if (test1 != "OK1") return "fail1: $test1"
    val test2 = test2()
    if (test2 != "123OK") return "fail2: $test2"
    val test3 = test3()
    if (test3 != "321OK") return "fail3: $test3"
    val test4 = test4()
    if (test4 != "OK456") return "fail4: $test4"
    val test5 = test5()
    if (test5 != "OK3") return "fail5: $test5"
    val test6 = test6()
    if (test6 != "OK137joo") return "fail6: $test6"
    val test7 = test7()
    if (test7 != "OKmer, nas73, 37") return "fail7: $test7"
    val test8 = E<Int>().foo(56)
    if (test8 != "OK56") return "fail8: $test8"
    val test9 = F<Int>().foo(65, "hello")
    if (test9 != "OK65hello") return "fail9: $test9"
    val test10 = test10<Int>()
    if (test10 != "OKnullabc") return "fail10: $test10"
    val test11 = test11<Int>()
    if (test11 != "OKnulldef") return "fail11: $test11"

    return "OK"
}
