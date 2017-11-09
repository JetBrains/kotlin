// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
