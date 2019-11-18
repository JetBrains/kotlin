// IGNORE_BACKEND_FIR: JVM_IR
object A {
    private val p = "OK";

    object B {
        val z = p;
    }

}

fun box(): String {
    return A.B.z
}
