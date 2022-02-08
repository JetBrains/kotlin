// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

// WITH_STDLIB

fun intRangeTo(a: Int, b: Int) { for (i in a .. b) {} }
fun intRangeToStep(a: Int, b: Int) { for (i in a .. b step 127) {} }  // Uses IADD in non-IR
fun intDownTo(a: Int, b: Int) { for (i in a downTo b) {} }
fun intDownToStep(a: Int, b: Int) { for (i in a downTo b step 128) {} }  // Uses IADD in non-IR
fun intUntil(a: Int, b: Int) { for (i in a until b) {} }

fun byteRangeToByte(a: Byte, b: Byte) { for (i in a .. b) {} }
fun byteRangeToShort(a: Byte, b: Short) { for (i in a .. b) {} }
fun byteRangeToInt(a: Byte, b: Int) { for (i in a .. b) {} }
fun shortRangeToByte(a: Short, b: Byte) { for (i in a .. b) {} }
fun shortRangeToShort(a: Short, b: Short) { for (i in a .. b) {} }
fun shortRangeToInt(a: Short, b: Int) { for (i in a .. b) {} }
fun intRangeToByte(a: Int, b: Byte) { for (i in a .. b) {} }
fun intRangeToShort(a: Int, b: Short) { for (i in a .. b) {} }

fun uIntRangeTo(a: UInt, b: UInt) { for (i in a .. b) {} }
fun uIntRangeToStep(a: UInt, b: UInt) { for (i in a .. b step 127) {} }  // Uses IADD in non-IR
fun uIntDownTo(a: UInt, b: UInt) { for (i in a downTo b) {} }
fun uIntDownToStep(a: UInt, b: UInt) { for (i in a downTo b step 128) {} }  // Uses IADD in non-IR
fun uIntUntil(a: UInt, b: UInt) { for (i in a until b) {} }

fun uByteRangeTo(a: UByte, b: UByte) { for (i in a .. b) {} }
fun uShortRangeTo(a: UShort, b: UShort) { for (i in a .. b) {} }

// 0 ISUB

// JVM_TEMPLATES
// 4 IADD
// 16 IINC

// JVM_IR_TEMPLATES
// 122 ILOAD
// 42 ISTORE
// 0 IADD
// 0 ISUB
// 20 IINC