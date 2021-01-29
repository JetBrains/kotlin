// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// FULL_JDK

fun lambdaIsSerializable(fn: () -> Unit) = fn is java.io.Serializable

fun box(): String {
    if (lambdaIsSerializable {})
        return "Failed: indy lambdas are not serializable"
    return "OK"
}