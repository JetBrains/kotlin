// CHECK_BYTECODE_LISTING
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

fun equalsChecks(left: DPoint, right: DPoint) {
    require(100, left == right)
    require(101, left.toObject() == right)
    require(102, left == right.toObject())
    require(103, left.toObject() == right.toObject())
    require(104, null == right)
    require(105, left == null)
    require(106, null as Any? == right)
    require(107, left == null as Any?)
    require(108, null.toDPointOrNull() == right)
    require(109, left == null.toDPointOrNull())
    require(110, left.toDPointOrNull() == right)
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
// 1 BIPUSH 108\n {4}ACONST_NULL\n {4}.*toDPointOrNull.*\n {4}DUP\n {4}IFNONNULL.*(\n {3}([^b\n]|b[^o\n]|bo[^x\n]|box-impl\d)*)*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 109\n {4}DLOAD 0\n {4}DLOAD 2\n {4}ACONST_NULL\n {4}.*toDPointOrNull.*\n {4}INVOKESTATIC .*equals-impl .*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 110\n {4}DLOAD 0\n {4}DLOAD 2\n {4}.*toDPointOrNull.*\n {4}DUP\n {4}IFNONNULL.*(\n {3}([^b\n]|b[^o\n]|bo[^x\n]|box-impl\d)*)*\n {4}INVOKESTATIC .*require
// 1 BIPUSH 111\n {4}DLOAD 0\n {4}DLOAD 2\n {4}DLOAD 4\n {4}DLOAD 6\n {4}.*toDPointOrNull.*\n {4}INVOKESTATIC .*equals-impl .*\n {4}INVOKESTATIC .*require
