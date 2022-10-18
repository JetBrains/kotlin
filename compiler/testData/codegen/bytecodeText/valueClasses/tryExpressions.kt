// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class DPoint(val x: Double, val y: Double)

fun tryExpr() = try {
    DPoint(0.0, 1.0)
} catch(_: Throwable) {
    DPoint(2.0, 3.0)
} finally {
    DPoint(4.0, 5.0)
}

fun tryBody() {
    try {
        DPoint(0.0, 1.0)
    } catch(_: Throwable) {
        DPoint(2.0, 3.0)
    } finally {
        DPoint(4.0, 5.0)
    }
    val x: DPoint = try {
        DPoint(0.0, 1.0)
    } catch(_: Throwable) {
        DPoint(2.0, 3.0)
    } finally {
        DPoint(4.0, 5.0)
    }
}


// 1 tryExpr.*(\n  .*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)
// 0 tryExpr.*(\n  .*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)
// 0 tryBody.*(\n   .*)*(\n   .*box-impl.*)
