package j

interface MyFunc<T> {}

class A(val b: B) {
}

class B {
    operator fun <T> invoke(<!UNUSED_PARAMETER!>f<!>: (T) -> T): MyFunc<T> = throw Exception()
}

fun <R> id(r: R) = r

fun foo(a: A) {
    val <!UNUSED_VARIABLE!>r<!> : MyFunc<Int> = id (a.b { x -> x + 14 })
}