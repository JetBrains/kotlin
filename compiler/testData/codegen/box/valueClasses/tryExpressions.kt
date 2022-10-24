// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class DPoint(val x: Double, val y: Double) {
    init {
        require(x != 0.0 && y != 0.0)
    }
}

fun tryOk() = try {
    DPoint(1.0, 2.0)
} catch(_: Throwable) {
    DPoint(0.0, 3.0)
} finally {
    DPoint(4.0, 5.0)
}

fun tryFail1() = try {
    DPoint(0.0, 1.0)
} catch(_: Throwable) {
    DPoint(2.0, 3.0)
} finally {
    DPoint(4.0, 5.0)
}

fun tryFail2() = try {
    DPoint(1.0, 2.0)
} catch(_: Throwable) {
    DPoint(3.0, 4.0)
} finally {
    DPoint(5.0, 0.0)
}

fun box(): String {
    require(runCatching { tryOk() } == Result.success(DPoint(1.0, 2.0)))
    require(runCatching { tryFail1() } == Result.success(DPoint(2.0, 3.0)))
    require(runCatching { tryFail2() }.isFailure)
    return "OK"
}
