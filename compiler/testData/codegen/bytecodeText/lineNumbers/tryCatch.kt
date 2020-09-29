// IGNORE_BACKEND_FIR: JVM_IR
fun foo() {
    try {
        System.out?.println()
    } catch(e: Exception) {
        System.out?.println()
    }
}
// 1 LINENUMBER 2