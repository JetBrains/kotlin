// IGNORE_BACKEND_FIR: JVM_IR
data class A(val o: String, val k: String) {
    constructor() : this("O", "k")
}

fun box(): String {
    val a = A().copy(k = "K")
    return a.o + a.k
}

