// IMPORTANT!
// Please, when your changes cause failures in bytecodeText tests for 'for' loops,
// examine the resulting bytecode shape carefully.
// Range and progression-based loops generated with Kotlin compiler should be
// as close as possible to Java counter loops ('for (int i = a; i < b; ++i) { ... }').
// Otherwise it may result in performance regression due to missing HotSpot optimizations.
// Run Kotlin compiler benchmarks (https://github.com/Kotlin/kotlin-benchmarks)
// with compiler built from your changes if you are not sure.

fun Int.toTrue() = true

fun testBooleanArray(n: Int) =
        BooleanArray(n) { it.toTrue() }

fun testByteArray(n: Int) =
        ByteArray(n) { it.toByte() }

fun testShortArray(n: Int) =
        ShortArray(n) { it.toShort() }

fun testIntArray(n: Int) =
        IntArray(n) { it }

fun testLongArray(n: Int) =
        LongArray(n) { it.toLong() }

fun testFloatArray(n: Int) =
        FloatArray(n) { it.toFloat() }

fun testDoubleArray(n: Int) =
        DoubleArray(n) { it.toDouble() }

fun testObjectArray(n: Int) =
        Array(n) { it.toString() }

// 0 IF_ICMPGT
// 0 IF_CMPEQ
// 8 IF_ICMPGE

// JVM_IR_TEMPLATES
// 48 ILOAD
// 16 ISTORE
// 0 IADD
// 0 ISUB
// 8 IINC
