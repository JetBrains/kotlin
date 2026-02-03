// IGNORE_BACKEND_K2_MULTI_MODULE: ANY
// ^^^ Cannot split to two modules due to cyclic import
// FILE: test.kt

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

class V {
    fun memberFun(): String = ""
}

fun topLevelFun(): String = ""

val refInVariable = ::topLevelFun

fun box(): String {
    val v0 = V()
    val v1 = V()
    fun localFun1(): String = ""
    fun localFun2(): String = ""

    checkEqual(::topLevelFun, ::topLevelFun)
    checkEqual(::topLevelFun, referenceTopLevelFunFromOtherFile())

    checkEqual(V::memberFun, V::memberFun)
    checkEqual(v0::memberFun, v0::memberFun)

    checkNotEqual(v0::memberFun, V::memberFun)
    checkNotEqual(v0::memberFun, v1::memberFun)

    if (::topLevelFun === ::topLevelFun) throw AssertionError("::topLevelFun should not be identity-equal to ::topLevelFun")

    // Saved reference in variable
    checkEqual(refInVariable, ::topLevelFun)

    // hashCode stability
    val ref = ::topLevelFun
    if (ref.hashCode() != ref.hashCode()) throw AssertionError("hashCode should be stable")

    checkEqual(::localFun1, ::localFun1)
    checkNotEqual(::localFun1, ::localFun2)

    return "OK"
}

// FILE: fromOtherFile.kt

fun referenceTopLevelFunFromOtherFile() = ::topLevelFun
