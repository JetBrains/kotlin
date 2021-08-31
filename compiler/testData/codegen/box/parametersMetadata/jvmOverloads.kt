// SKIP_JDK6
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// PARAMETERS_METADATA
// KT-23628

class A @JvmOverloads constructor(x: String, y: Int = 42) {
    @JvmOverloads
    fun f(a: Long, b: Char = 'b') {}
}

fun box(): String {
    val ctor = A::class.java.getDeclaredConstructor(String::class.java).parameters.toList()
    if (ctor.toString() != "[java.lang.String x]") return "Fail constructor: $ctor"

    val method = A::class.java.getDeclaredMethod("f", Long::class.java).parameters.toList()
    if (method.toString() != "[long a]") return "Fail method: $method"

    return "OK"
}
