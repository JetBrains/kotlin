// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

fun box(): String {
    var x = 0
    run { x++ }
    run { ++x }
    return if (x == 2) "OK" else "Fail: $x"
}