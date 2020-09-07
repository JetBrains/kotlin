// FILE: test.kt
class A(val value: String)

fun box(): String {
    val ref = A::value
    return ref(A("OK"))
}

// Check that non-bound callable references are generated as singletons
// 1 GETSTATIC TestKt\$box\$ref\$1.INSTANCE
