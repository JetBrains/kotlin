//package test.render

fun test1() {}
fun test2(a: String?) {}
fun <T, U> test3(t: T, u: U) {}
fun <T: String> test4(t: T) {}
fun test5(): String = "some"
fun test6(): Comparable<String> = "some"

fun String.extension1() {}
fun <T> Comparable<T>.extension2() {}

val a: Int = 1
val <T> Comparable<T>.a = "String"
val b = object {}

class A1
class A2(val a: Int, var b = "some")
class A3(val a: Int) {
    var b = "some"
}
class A4<T: Any>(val t: T?)
class A5 {
    class Inner1
    inner class Inner2
}
class A6 {
    default object {
        fun test() {}
    }
}
class A7 {
    val a: Int

    {
        a = 1
    }
}

enum class Enum1 {
    FIRST
    SECOND
}

trait Trait
trait Trait1: Trait

class TestWithWhere<T> where T: Any?
fun <T> testWithWhere() where T: String {}

class WithDefaultArgs(val a: Int = 1, b: String = "str")
fun withDefaulArgs(a: Int = 1, b: String = "str") {}


