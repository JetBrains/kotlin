// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

class A

fun box(): String {
    val a1 = A::class.java.kotlin
    val a2 = A::class

    if (a1 != a2) return "Fail equals"
    if (a1.hashCode() != a2.hashCode()) return "Fail hashCode"
    if (a1.toString() != a2.toString()) return "Fail toString"

    return "OK"
}
