// FILE: main.kt
package a.b.c

class Outer {
    class Inner {
        fun foo(): Class<*> {
            class Outer
            return <expr>a.b.c.Outer.Inner::class.java</expr>
        }
        companion object {
        }
    }
}