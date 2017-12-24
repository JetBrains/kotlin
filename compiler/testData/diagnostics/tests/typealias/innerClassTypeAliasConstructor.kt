// !WITH_NEW_INFERENCE
class Outer {
    inner class Inner
}

typealias OI = Outer.Inner

fun test1(x: Outer) = x.OI()


class Generic<T> {
    inner class Inner
}

typealias GI<T> = Generic<T>.Inner
typealias GIntI = Generic<Int>.Inner

fun test2(x: Generic<Int>) = x.GI()
fun <T> test3(x: Generic<T>) = x.GI()
fun <T> test4(x: Generic<List<T>>) = x.GI()
fun <T> test5(x: Generic<T>) = <!TYPE_MISMATCH!>x<!>.GIntI()
fun Generic<Int>.test6() = GIntI()