// FILE: a.kt

package a.b.c

class D {
    fun foo() = 42
}

// FILE: b.kt

fun main() {
    val x = a.b.c.D::foo

    x : KMemberFunction0<a.b.c.D, Int>
}
