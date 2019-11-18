// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun foo(): List<String>? = listOf("abcde")

fun box(): String {
    for (i in 1..3) {
        for (value in foo() ?: continue) {
            if (value != "abcde") return "Fail"
        }
    }
    return "OK"
}
