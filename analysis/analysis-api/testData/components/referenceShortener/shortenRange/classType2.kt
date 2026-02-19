// FILE: main.kt
package a.b.c

class Outer {
    class Middle {
        class Inner {
            fun foo(): Class<*> {
                return <expr>a.b.c.Outer.Middle.Inner::class.java</expr>
            }
        }
    }
}