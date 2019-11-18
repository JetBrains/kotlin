// IGNORE_BACKEND_FIR: JVM_IR
class A {
    val value: String
        get() = field + "K"

    constructor(o: String) {
        value = o
    }
}

fun box() = A("O").value