// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    fun <T> foo(t:() -> T) : T = t()

    return foo {"OK"}
}