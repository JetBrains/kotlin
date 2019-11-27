// IGNORE_BACKEND_FIR: JVM_IR
class A {
}

operator fun A.component1() = 1
operator fun A.component2() = 2

fun box() : String {
    var (a, b) = A()

    fun local() {
        a = 3
    }
    local()
    return if (a == 3 && b == 2) "OK" else "fail"
}