// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// The name of a parameter that holds the underlying value of an inline class is encoded in the bytecode.
// The message of its null check must show the source name.

@JvmInline
value class S(val s: String)

fun take(s: S) {}

fun S.extension() {}

fun nullCheckMessage(name: String): String? {
    val method = Class.forName("NullCheckMessageOfEncodedParameterNameKt").declaredMethods.single { it.name.startsWith("$name-") }
    try {
        method.invoke(null, null)
    } catch (e: java.lang.reflect.InvocationTargetException) {
        return e.cause?.message
    }
    return null
}

fun box(): String {
    val take = nullCheckMessage("take")
    if (take == null || !take.endsWith(", parameter s")) return "take: $take"
    val extension = nullCheckMessage("extension")
    if (extension == null || !extension.endsWith(", parameter \$this\$extension")) return "extension: $extension"
    return "OK"
}
