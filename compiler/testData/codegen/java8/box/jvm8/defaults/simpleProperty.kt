// !API_VERSION: 1.3
// !ENABLE_JVM_DEFAULT
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Z {
    @JvmDefault
    val z: String
        get() = "OK"
}


class Test : Z

fun box() : String {
    return Test().z
}