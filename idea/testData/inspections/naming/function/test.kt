fun Foo() {}

fun FOO_BAR() {}

fun xyzzy() {}

fun `a b`() {}

interface I {
    fun a_b()
}

class C : I {
    override fun a_b() {} // Shouldn't be reported
}

fun Vector3d(): Int = 42

interface D
fun D(): D = object : D {}

interface E
fun E() = object : E {}

typealias F = () -> String
fun F(): F = { "" }
