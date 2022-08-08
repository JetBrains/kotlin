// TARGET_BACKEND: JVM_IR
// FULL_JDK
// WITH_REFLECT

class A

fun box(): String {
    val clz = A::class
    System.gc()
    val clz2 = A::class
    if (clz === clz2) return "OK"
    return "Fail"
}
