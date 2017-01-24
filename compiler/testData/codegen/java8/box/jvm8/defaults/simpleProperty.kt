// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS

interface Z {
    val z: String
        get() = "OK"
}


class Test : Z

fun box() : String {
    return Test().z
}