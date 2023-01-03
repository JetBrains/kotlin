val x: dynamic = 23

interface I {
    fun foo(): String
}

class C : I by x

object O : I by x

fun box(): String {
    return object : I by x {}.foo()
}
