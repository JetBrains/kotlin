// IGNORE_BACKEND_FIR: JVM_IR
fun foo(): Int {
    if (true) {
        return 1
    } else {
        return 2
    }
}
// 1 LINENUMBER 2
