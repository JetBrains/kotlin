// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE
package test

object Wrong
object Right

class a {
    class b<T> {
        class c {
            fun foo() = Wrong
        }
    }
}

fun Int.foo() = Right

class Test {
    val a: List<Int> = null!!

    val <T> List<T>.b: Int get() = 42

    val Int.c: Int get() = 42

    val test1: () -> Right = a.b<Int>.c::foo
    val test1a: () -> Right = a.b.c::foo

    val test2: () -> Right = a.b<Int>.c?::foo
}
