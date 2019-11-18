// IGNORE_BACKEND_FIR: JVM_IR
open class Base(val fn1: () -> String, val fn2: () -> String)

fun box(): String {
    val x = "x"

    class Local(y: String) : Base({ x + y }, { y + x })

    val local = Local("y")
    val z1 = local.fn1()
    val z2 = local.fn2()

    if (z1 != "xy") return "Fail: z1=$z1"
    if (z2 != "yx") return "Fail: z2=$z2"

    return "OK"
}