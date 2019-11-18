// IGNORE_BACKEND_FIR: JVM_IR
open class A {
    internal open val field = "AF"

    internal open fun test(): String = "AM"
}

fun invokeOnA(a: A) = a.test() + a.field

class Z : A() {
    override val field: String = "ZF"

    override fun test(): String = "ZM"
}

fun box() : String {
    var invokeOnA = invokeOnA(A())
    if (invokeOnA != "AMAF") return "fail 1: $invokeOnA"

    invokeOnA = invokeOnA(Z())
    if (invokeOnA != "ZMZF") return "fail 2: $invokeOnA"

    val z = Z().test()
    if (z != "ZM") return "fail 3: $z"

    val f = Z().field
    if (f != "ZF") return "fail 4: $f"

    return "OK"
}