// IGNORE_BACKEND_FIR: JVM_IR
fun foo() : String {
    val u = {
        class B(val data : String)
        B("OK").data
    }
    return u()
}

fun box(): String {
    return foo()
}