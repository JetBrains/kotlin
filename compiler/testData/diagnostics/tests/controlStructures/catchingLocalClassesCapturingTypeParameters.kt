// !LANGUAGE: +ProhibitInnerClassesOfGenericClassExtendingThrowable
package test

var global: Throwable? = null

fun <T> foo(x: Throwable, z: T, b: (T) -> Unit) {
    <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class A<!>(val y : T) : Exception()

    try {
        throw x
    } catch (a: A) {
        b(a.y)
    } catch (e: Throwable) {
        global = A(z)
    }
}

fun main() {
    foo(RuntimeException(), 1) { throw IllegalStateException() }
    foo(global!!, "") { it.length } // (*)
}

// (*):
//Exception in thread "main" java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String
//  at test.TestKt$main$2.invoke(test.kt)
//  at test.TestKt.foo(test.kt:12)
//  at test.TestKt.main(test.kt:21)