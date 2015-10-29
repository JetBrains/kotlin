// MODULE: m1
// FILE: a.kt
package a

class b {
    fun a_b() {}
}

// MODULE: m2
// FILE: b.kt
class a {
    class b {
        fun _ab() {}
    }
}

// MODULE: m3(m1, m2)
// FILE: c1.kt
package some

fun test(a_b: a.b) {
    a_b.a_b()

    val a_b2 = a.b()
    a_b2.a_b()
}

// FILE: c2.kt
package a

fun test(a_b: a.b) {
    a_b.a_b()

    val a_b2 = a.b()
    a_b2.a_b()
}

// FILE: c3.kt
fun test(_ab: a.b) {
    _ab._ab()

    val _ab2 = a.b()
    _ab2._ab() // todo
}