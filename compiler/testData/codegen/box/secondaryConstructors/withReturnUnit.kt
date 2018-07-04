// IGNORE_BACKEND: JVM_IR
class A {
    val prop: Int
    constructor(arg: Boolean) {
        if (arg) {
            prop = 1
            return Unit
        }
        prop = 2
    }
}

fun box(): String {
    val a1 = A(true)
    if (a1.prop != 1) return "fail1: ${a1.prop}"
    val a2 = A(false)
    if (a2.prop != 2) return "fail2: ${a2.prop}"
    return "OK"
}
