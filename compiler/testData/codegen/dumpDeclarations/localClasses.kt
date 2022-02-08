// WITH_STDLIB
// FILE: Part1.kt
class A {
    fun a() : String {
        class B() {
            fun s() : String = "OK"

            inner class C {}

        }
        return B().s()
    }
}


class B {
    fun a(p: String) : String {
        class B() {
            fun s() : String = p
        }
        return B().s()
    }
}

class L {
    fun a(lambda: () -> Unit) = lambda()

    inline fun b() {
        a {
            println("OK")
        }
    }

}

// FILE: Part2.kt
fun box() {
    L().b()
}
