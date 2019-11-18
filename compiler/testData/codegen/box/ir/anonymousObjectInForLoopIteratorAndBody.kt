// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    var result = ""
    for (x in listOf('O', 'A', 'K').filter { it > 'D' }) {
        result += object { fun run() = x }.run()
    }
    return result
}
