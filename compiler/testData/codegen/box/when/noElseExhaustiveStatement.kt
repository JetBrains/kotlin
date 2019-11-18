// IGNORE_BACKEND_FIR: JVM_IR
enum class En {
    A,
    B
}

fun box(): String {
    when(En.A) {
        En.A -> "s1"
        En.B -> "s2"
    }
    return "OK"
}