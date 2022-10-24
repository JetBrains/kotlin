// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class DPoint(val x: Double, val y: Double)

fun ifExpr() = if (DPoint(0.0, 1.0).x > 0.0) DPoint(2.0, 3.0) else DPoint(4.0, 5.0)

fun whenExpr() = when {
    DPoint(6.0, 7.0).x > 0.0 -> DPoint(8.0, 9.0)
    DPoint(10.0, 11.0).x > 0.0 -> DPoint(12.0, 13.0)
    else -> DPoint(14.0, 15.0)
}

fun ifBody() {
    if (DPoint(0.0, 1.0).x > 0.0) DPoint(2.0, 3.0) else DPoint(4.0, 5.0)
    val x = if (DPoint(0.0, 1.0).x > 0.0) DPoint(2.0, 3.0) else DPoint(4.0, 5.0)
}

fun whenBody() {
    when {
        DPoint(6.0, 7.0).x > 0.0 -> DPoint(8.0, 9.0)
        DPoint(10.0, 11.0).x > 0.0 -> DPoint(12.0, 13.0)
        else -> DPoint(14.0, 15.0)
    }
    val x = when {
        DPoint(6.0, 7.0).x > 0.0 -> DPoint(8.0, 9.0)
        DPoint(10.0, 11.0).x > 0.0 -> DPoint(12.0, 13.0)
        else -> DPoint(14.0, 15.0)
    }
}

// 1 ifExpr.*(\n  .*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)
// 0 ifExpr.*(\n  .*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)
// 1 whenExpr.*(\n  .*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)
// 0 whenExpr.*(\n  .*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)(\n   .*)*(\n   .*box-impl.*)
// 0 ifBody.*(\n   .*)*(\n   .*box-impl.*)
// 0 whenBody.*(\n   .*)*(\n   .*box-impl.*)
