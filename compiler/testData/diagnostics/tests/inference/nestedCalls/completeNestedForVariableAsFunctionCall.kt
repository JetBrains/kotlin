// FIR_IDENTICAL
package j

interface MyFunc<T> {}

class A(val b: B) {
}

class B {
    operator fun <T> invoke(f: (T) -> T): MyFunc<T> = throw Exception()
}

fun <R> id(r: R) = r

fun foo(a: A) {
    val r : MyFunc<Int> = id (a.b { x -> x + 14 })
}
