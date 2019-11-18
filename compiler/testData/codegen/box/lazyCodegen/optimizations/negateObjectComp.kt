// IGNORE_BACKEND_FIR: JVM_IR
val p: Int? = 1;
val z: Int? = 2;

fun box(): String {
    if (!(p!! == z!!)) {
        return "OK"
    }
    return "fail"
}