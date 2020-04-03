// IGNORE_BACKEND: JVM_IR
// TODO KT-36829 Optimize 'in' expressions in JVM_IR

fun testUIntRangeLiteral(a: UInt, b: UInt) = 42u in a .. b

fun testULongRangeLiteral(a: ULong, b: ULong) = 42UL in a .. b

fun testUIntUntil(a: UInt, b: UInt) = 42u in a until b

fun testULongUntil(a: ULong, b: ULong) = 42UL in a until b

// 0 contains
