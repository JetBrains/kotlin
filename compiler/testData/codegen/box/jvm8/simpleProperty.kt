// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8

interface Z {
    val z: String
        get() = "OK"
}


class Test : Z

fun box() : String {
    return Test().z
}