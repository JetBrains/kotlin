// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

fun nil() = null

fun list() = java.util.Arrays.asList("1")

fun box(): String {
    for (x in nil()?:list()) {
    }
    return "OK"
}
