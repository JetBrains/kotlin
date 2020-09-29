// IGNORE_BACKEND_FIR: JVM_IR
fun foo(): Int {
    if (false) {
        return 1
    } else {
        return 2
    }
}
// 1 LINENUMBER 2
