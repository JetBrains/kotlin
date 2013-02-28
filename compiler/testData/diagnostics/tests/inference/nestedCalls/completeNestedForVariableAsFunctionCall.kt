package j

trait MyFunc<T> {}

class A(val b: B) {
}

class B {
    fun <T> invoke(<!UNUSED_PARAMETER!>f<!>: (T) -> T): MyFunc<T> = throw Exception()
}

fun id<R>(r: R) = r

fun foo(a: A) {
    val <!UNUSED_VARIABLE!>r<!> : MyFunc<Int> = id (a.b { x -> x + 14 })
}