// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: FAILS_IN_JS_IR
// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// FILE: test.kt

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

class V {
    val memberVal: String = ""
    fun memberFun(): String = ""
}

val topLevelVar: String = ""
fun topLevelFun(): String = ""

fun box(): String {
    val v0 = V()
    val v1 = V()

    checkEqual(::topLevelFun, ::topLevelFun)
    checkEqual(::topLevelFun, referenceTopLevelFunFromOtherFile())
    checkEqual(::topLevelVar, ::topLevelVar)
    checkEqual(::topLevelVar, referenceTopLevelVarFromOtherFile())

    checkEqual(V::memberFun, V::memberFun)
    checkEqual(v0::memberFun, v0::memberFun)
    checkEqual(V::memberVal, V::memberVal)
    checkEqual(v0::memberVal, v0::memberVal)

    checkNotEqual(v0::memberFun, V::memberFun)
    checkNotEqual(v0::memberVal, V::memberVal)
    checkNotEqual(v0::memberFun, v1::memberFun)
    checkNotEqual(v0::memberVal, v1::memberVal)

    return "OK"
}

// FILE: fromOtherFile.kt

fun referenceTopLevelFunFromOtherFile() = ::topLevelFun
fun referenceTopLevelVarFromOtherFile() = ::topLevelVar
