// FILE: main.kt
package a.b.c

class Outer {
    class Outer
    class Inner {
        fun foo(): Class<*> {
            return <expr>a.b.c.Outer.Inner::class.java</expr>
        }
        companion object {
        }
    }
}