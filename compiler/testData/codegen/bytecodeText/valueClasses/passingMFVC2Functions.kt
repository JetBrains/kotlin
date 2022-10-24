// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class DPoint(val x: Double, val y: Double)

fun acceptBoxed(x: Any?) {}
fun acceptFlattened(x: DPoint) {}
fun returnBoxed() = DPoint(3.0, 4.0)

fun testFlattened2Boxed() {
    acceptBoxed(DPoint(1.0, 2.0))
}

fun testBoxed2Boxed() {
    acceptBoxed(returnBoxed())
}

fun testFlattened2Flattened() {
    acceptFlattened(DPoint(1.0, 2.0))
}

fun testBoxed2Flattened() {
    acceptFlattened(returnBoxed())
}

fun testIgnoredFlattened() {
    DPoint(1.0, 2.0)
    DPoint(1.0, 2.0)
}

fun testIgnoredBoxed() {
    returnBoxed()
}

// 1 testFlattened2Boxed\(\)V(\n {3}.*)*((\n {3}.*box-impl .*)(\n {3}.*)*){1}
// 0 testFlattened2Boxed\(\)V(\n {3}.*)*((\n {3}.*box-impl.*)(\n {3}.*)*){2}
// 0 testBoxed2Boxed\(\)V(\n {3}.*)*((\n {3}.*(box-impl|DSTORE|DLOAD).*)(\n {3}.*)*){1}
// 0 testFlattened2Flattened\(\)V(\n {3}.*)*((\n {3}.*box-impl.*)(\n {3}.*)*){1}
// 1 testFlattened2Flattened\(\)V(\n {3}.*)*((\n {3}.*DSTORE.*)(\n {3}.*)*){2}
// 0 testFlattened2Flattened\(\)V(\n {3}.*)*((\n {3}.*DSTORE.*)(\n {3}.*)*){3}
// 0 testBoxed2Flattened\(\)V(\n {3}.*)*((\n {3}.*box-impl .*)(\n {3}.*)*){1}
// 1 testBoxed2Flattened\(\)V(\n {3}.*)*((\n {3}.*unbox-impl.*)(\n {3}.*)*){2}
// 0 testBoxed2Flattened\(\)V(\n {3}.*)*((\n {3}.*unbox-impl.*)(\n {3}.*)*){3}
// 0 testIgnoredFlattened\(\)V(\n {3}.*)*((\n {3}.*box-impl.*)(\n {3}.*)*){1}
// 0 testIgnoredBoxed\(\)V(\n {3}.*)*((\n {3}.*box-impl.*)(\n {3}.*)*){1}
