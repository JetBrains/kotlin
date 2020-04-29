// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    internal open val field = "F"

    internal open fun test(): String = "A"
}

class Z : A() {
    override fun test(): String = super.test()

    override val field = super.field
}

fun box() : String {
    val z = Z().test()
    if (z != "A") return "fail 1: $z"

    val f = Z().field
    if (f != "F") return "fail 2: $f"

    return "OK"
}