// TARGET_BACKEND: JVM

// WITH_STDLIB

@JvmField public val publicField = "1";
@JvmField internal val internalField = "23";

fun <T> eval(fn: () -> T) = fn()

fun test(): String {
    return eval {
        publicField + internalField
    }
}

fun box(): String {
    return if (test() == "123") return "OK" else "fail"
}
