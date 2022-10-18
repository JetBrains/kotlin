// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class DPoint(val x: Double, val y: Double)

fun require(index: Int, condition: Boolean) = require(condition) { "$index" }

fun DPoint.toObject() = this as Any
fun DPoint.toDPointOrNull() = this as DPoint?
fun Nothing?.toDPointOrNull() = this as DPoint?

// separate to escape from StackOverflow in regex

fun equalsChecks1(left: DPoint, right: DPoint) {
    require(100, left == right)
}

fun equalsChecks2(left: DPoint, right: DPoint) {
    require(101, left.toObject() == right)
}
fun equalsChecks3(left: DPoint, right: DPoint) {
    require(102, left == right.toObject())
}
fun equalsChecks4(left: DPoint, right: DPoint) {
    require(103, left.toObject() == right.toObject())
}
fun equalsChecks5(left: DPoint, right: DPoint) {
    require(104, null == right)
}
fun equalsChecks6(left: DPoint, right: DPoint) {
    require(105, left == null)
}
fun equalsChecks7(left: DPoint, right: DPoint) {
    require(106, null as Any? == right)
}
fun equalsChecks8(left: DPoint, right: DPoint) {
    require(107, left == null as Any?)
}
fun equalsChecks9(left: DPoint, right: DPoint) {
    require(108, null.toDPointOrNull() == right)
}
fun equalsChecks10(left: DPoint, right: DPoint) {
    require(109, left == null.toDPointOrNull())
}
fun equalsChecks11(left: DPoint, right: DPoint) {
    require(110, left.toDPointOrNull() == right)
}
fun equalsChecks12(left: DPoint, right: DPoint) {
    require(111, left == right.toDPointOrNull())
}

// 1 BIPUSH 100\n {4}DLOAD 0\n {4}DLOAD 2\n {4}DLOAD 4\n {4}DLOAD 6\n {4}INVOKESTATIC .*equals-impl0.*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 101\n {4}DLOAD 0\n {4}DLOAD 2\n {4}.*toObject.*\n {4}DLOAD 4\n {4}DLOAD 6\n {4}.*box-impl.*\n {4}INVOKESTATIC .*Intrinsics.areEqual.*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 102\n {4}DLOAD 0\n {4}DLOAD 2\n {4}DLOAD 4\n {4}DLOAD 6\n {4}.*toObject.*\n {4}INVOKESTATIC .*equals-impl .*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 103\n {4}DLOAD 0\n {4}DLOAD 2\n {4}.*toObject.*\n {4}DLOAD 4\n {4}DLOAD 6\n {4}.*toObject.*\n {4}INVOKESTATIC .*Intrinsics.areEqual.*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 104\n {4}ICONST_0\n {4}INVOKESTATIC .*require
// 1 BIPUSH 105\n {4}DLOAD 0\n {4}DLOAD 2\n {4}ACONST_NULL\n {4}INVOKESTATIC .*equals-impl .*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 106\n {4}ACONST_NULL\n {4}DLOAD 4\n {4}DLOAD 6\n {4}.*box.*\n {4}INVOKESTATIC .*Intrinsics.areEqual.*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 107\n {4}DLOAD 0\n {4}DLOAD 2\n {4}ACONST_NULL\n {4}INVOKESTATIC .*equals-impl .*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 108\n {4}ACONST_NULL\n {4}.*toDPointOrNull.*\n {4}ASTORE.*\n {4}ALOAD.*\n {4}DUP\n {4}IFNONNULL.*(\n {3}([^b\n]|b[^o\n]|bo[^x\n]|box-impl-\d)+)*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 109\n {4}DLOAD 0\n {4}DLOAD 2\n {4}ACONST_NULL\n {4}.*toDPointOrNull.*\n {4}INVOKESTATIC .*equals-impl .*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 110\n {4}DLOAD 0\n {4}DLOAD 2\n {4}.*toDPointOrNull.*\n {4}ASTORE.*\n {4}ALOAD.*\n {4}DUP\n {4}IFNONNULL.*(\n {3}([^b\n]|b[^o\n]|bo[^x\n]|box-impl-\d)+)*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 111\n {4}DLOAD 0\n {4}DLOAD 2\n {4}DLOAD 4\n {4}DLOAD 6\n {4}.*toDPointOrNull.*\n {4}INVOKESTATIC .*equals-impl .*\n {4}INVOKESTATIC .*require
