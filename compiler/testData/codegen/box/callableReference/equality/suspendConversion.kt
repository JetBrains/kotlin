// LANGUAGE: +SuspendConversion
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// FILE: suspendCovnersion.kt


fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}
fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}


fun capturePlain(fn: () -> Unit): Any = fn
fun captureSuspend(fn: suspend () -> Unit): Any = fn
fun capturePlainInt(fn: (Int) -> Unit): Any = fn
fun captureSuspendInt(fn: suspend (Int) -> Unit): Any = fn

fun foo() {}

class C {
    fun memberFun() {}
}

fun fnWithVararg(vararg xs: Int) {}

fun fnWithDefault(x1: Int = 1, x2: Int = 2) {}

fun fnReturnsInt() = 1

fun box(): String {
    val c = C()

    checkNotEqual(capturePlain(::foo), captureSuspend(::foo))
    checkEqual(capturePlain(::foo), capturePlain(::foo))
    checkEqual(captureSuspend(::foo), captureSuspend(::foo))
    checkNotEqual(capturePlain(c::memberFun), captureSuspend(c::memberFun))
    checkEqual(capturePlain(c::memberFun), capturePlain(c::memberFun))
    checkEqual(captureSuspend(c::memberFun), captureSuspend(c::memberFun))

    checkNotEqual(captureOther1(), captureOther2())
    checkNotEqual(captureBoundOther1(c), captureBoundOther2(c))

    checkNotEqual(capturePlainInt(::fnWithVararg), captureSuspendInt(::fnWithVararg))
    checkNotEqual(captureSuspend(::fnWithVararg), captureSuspendInt(::fnWithVararg))
    checkEqual(capturePlainInt(::fnWithVararg), capturePlainInt(::fnWithVararg))
    checkEqual(captureSuspendInt(::fnWithVararg), captureSuspendInt(::fnWithVararg))
    checkEqual(captureSuspend(::fnWithVararg), captureSuspend(::fnWithVararg))

    checkNotEqual(capturePlainInt(::fnWithDefault), captureSuspendInt(::fnWithDefault))
    checkNotEqual(captureSuspend(::fnWithDefault), captureSuspendInt(::fnWithDefault))
    checkEqual(capturePlainInt(::fnWithDefault), capturePlainInt(::fnWithDefault))
    checkEqual(captureSuspendInt(::fnWithDefault), captureSuspendInt(::fnWithDefault))
    checkEqual(captureSuspend(::fnWithDefault), captureSuspend(::fnWithDefault))

    checkNotEqual(capturePlain(::fnReturnsInt), captureSuspend(::fnReturnsInt))
    checkEqual(capturePlain(::fnReturnsInt), capturePlain(::fnReturnsInt))
    checkEqual(captureSuspend(::fnReturnsInt), captureSuspend(::fnReturnsInt))

    return "OK"
}

// FILE: fromOtherFile.kt

fun captureOther1() = capturePlain(::foo)
fun captureOther2() = captureSuspend(::foo)

fun captureBoundOther1(c: C) = capturePlain(c::memberFun)
fun captureBoundOther2(c: C) = captureSuspend(c::memberFun)