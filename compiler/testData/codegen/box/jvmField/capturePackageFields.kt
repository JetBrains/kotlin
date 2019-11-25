// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

@JvmField public val publicField = "1";
@JvmField internal val internalField = "23";

fun test(): String {
    return {
        publicField + internalField
    }()
}


fun box(): String {
    return if (test() == "123") return "OK" else "fail"
}
