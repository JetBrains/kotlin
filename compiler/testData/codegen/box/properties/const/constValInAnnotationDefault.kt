// TARGET_BACKEND: JVM
// WITH_STDLIB

// JVM_ABI_K1_K2_DIFF: K2 serializes annotation parameter default values (KT-59526).

const val z = "OK"

annotation class A(val value: String = z)

@A
class Test

fun box(): String {
    return Test::class.java.getAnnotation(A::class.java).value
}
