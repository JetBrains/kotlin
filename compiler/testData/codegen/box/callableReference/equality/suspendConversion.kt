// !LANGUAGE: +SuspendConversion
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR, JS_IR
// ^ TODO fix suspend coercion for bound function references in JVM_IR
// FILE: suspendCovnersion.kt

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
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
    checkNotEqual(capturePlain(c::memberFun), captureSuspend(c::memberFun))

    checkNotEqual(captureOther1(), captureOther2())
    checkNotEqual(captureBoundOther1(c), captureBoundOther2(c))

    checkNotEqual(capturePlainInt(::fnWithVararg), captureSuspendInt(::fnWithVararg))
    checkNotEqual(captureSuspend(::fnWithVararg), captureSuspendInt(::fnWithVararg))

    checkNotEqual(capturePlainInt(::fnWithDefault), captureSuspendInt(::fnWithDefault))
    checkNotEqual(captureSuspend(::fnWithDefault), captureSuspendInt(::fnWithDefault))

    checkNotEqual(capturePlain(::fnReturnsInt), captureSuspend(::fnReturnsInt))

    return "OK"
}

// FILE: fromOtherFile.kt

fun captureOther1() = capturePlain(::foo)
fun captureOther2() = captureSuspend(::foo)

fun captureBoundOther1(c: C) = capturePlain(c::memberFun)
fun captureBoundOther2(c: C) = captureSuspend(c::memberFun)