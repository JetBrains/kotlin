// FILE: main.kt
package a.b.c

class Outer {
    fun <E> E.foo(x: E, y: E, z: E) = x.hashCode() + y.hashCode() + z.hashCode()

    class Inner {
        fun foo(a: Int, b: Boolean, c: String) = c + a + b

        companion object {
            fun <T, E, D> foo(a: T, b: E, c: D) = a.hashCode() + b.hashCode() + c.hashCode()
        }
    }
}

fun test(): Int {
    fun foo(a: Int, b: Boolean, c: Int) = a + b.hashCode() + c
    return <expr>a.b.c.Outer.Inner.Companion.foo(1, false, "bar")</expr>
}