// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Z {
    @JvmDefault
    val z: String
        get() = "OK"
}


class Test : Z

fun box() : String {
    return Test().z
}
