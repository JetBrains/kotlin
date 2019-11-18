// IGNORE_BACKEND_FIR: JVM_IR
class A {
    var a: String = "Fail"

    init {
        a = object {
            override fun toString(): String = "OK"
        }.toString()
    }
}

fun box() : String {
    return A().a
}