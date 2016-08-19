import E.E1

object O {
    val y = 1
}

enum class E(val x: Int) {
    E1(0)
}

class C {
    companion object {
        val z = 2
    }
}

fun foo() = E1.x + O.y + C.z
