// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

class C @kotlin.jvm.JvmOverloads constructor(s1: String = "O", s2: String = "K") {
    public val status: String = s1 + s2
}

fun box(): String {
    val c = (C::class.java.getConstructor().newInstance())
    return c.status
}
