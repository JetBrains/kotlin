// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun foo() {
    if (1==1) {
        1.javaClass
    } else {
    }
}

fun box() = "OK"
