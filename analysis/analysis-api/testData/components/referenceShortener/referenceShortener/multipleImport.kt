// FILE: main.kt
package a.b.c

fun test(n: Int) {
    return if (<expr>x.y.z.Outer.Inner.VALUE0 > x.y.z.Outer.Inner.VALUE1</expr>) 1
    else n
}
// FILE: values.kt
package x.y.z

class Outer {
    object Inner {
        val VALUE0 = 13
        val VALUE1 = 17
    }
}