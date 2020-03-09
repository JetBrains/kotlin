// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

fun box(): String {
    val f = Any?::toString

    val owner = (f as kotlin.jvm.internal.CallableReference).owner as kotlin.jvm.internal.PackageReference
    if (owner.jClass.name != "kotlin.jvm.internal.Intrinsics\$Kotlin") return "Fail: $owner"

    return "OK"
}
