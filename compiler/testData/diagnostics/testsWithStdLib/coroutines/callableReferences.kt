// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE

suspend fun foo() {}

class A {
    suspend fun member() {}
}

suspend fun A.ext() {}

fun test1(a: A) {
    val x = ::foo

    val y1 = a::member
    val y2 = A::member

    val z1 = a::ext
    val z2 = A::ext
}

suspend fun test2(a: A) {
    val x = ::foo

    val y1 = a::member
    val y2 = A::member

    val z1 = a::ext
    val z2 = A::ext
}
