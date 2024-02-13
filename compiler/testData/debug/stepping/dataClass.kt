// IGNORE_BACKEND_K2: WASM
// FILE: test.kt

data class D(val i: Int, val s: String)

data class E(val i: Int, val s: String) {
    override fun toString() = "OK"
    override fun equals(other: Any?) = false
    override fun hashCode() = 42
    fun copy() = E(i, s)
}

fun box() {
    val d = D(1, "a")
    d.equals(D(1, "a"))
    d.hashCode()
    d.toString()
    val (i, s) = d
    d.copy()
    val e = E(1, "a")
    e.equals(E(1, "a"))
    e.hashCode()
    e.toString()
    val (s2, i2) = e
    e.copy()
}

// EXPECTATIONS JVM_IR
// test.kt:14 box
// test.kt:4 <init>
// test.kt:14 box
// test.kt:15 box
// test.kt:4 <init>
// test.kt:15 box
// test.kt:16 box
// test.kt:17 box
// test.kt:18 box
// test.kt:19 box
// test.kt:4 <init>
// test.kt:-1 copy
// test.kt:19 box
// test.kt:20 box
// test.kt:6 <init>
// test.kt:20 box
// test.kt:21 box
// test.kt:6 <init>
// test.kt:21 box
// test.kt:8 equals
// test.kt:21 box
// test.kt:22 box
// test.kt:9 hashCode
// test.kt:22 box
// test.kt:23 box
// test.kt:7 toString
// test.kt:23 box
// test.kt:24 box
// test.kt:25 box
// test.kt:10 copy
// test.kt:6 <init>
// test.kt:10 copy
// test.kt:25 box
// test.kt:26 box

// EXPECTATIONS ClassicFrontend JS_IR
// test.kt:14 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:1 equals
// test.kt:1 equals
// test.kt:1 equals
// test.kt:1 equals
// test.kt:1 equals
// test.kt:1 equals
// test.kt:16 box
// test.kt:1 hashCode
// test.kt:1 hashCode
// test.kt:17 box
// test.kt:1 toString
// test.kt:18 box
// test.kt:1 component1
// test.kt:18 box
// test.kt:1 component2
// test.kt:19 box
// test.kt:1 copy$default
// test.kt:1 copy$default
// test.kt:1 copy
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:20 box
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:21 box
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:21 box
// test.kt:8 equals
// test.kt:22 box
// test.kt:9 hashCode
// test.kt:23 box
// test.kt:7 toString
// test.kt:24 box
// test.kt:1 component1
// test.kt:24 box
// test.kt:1 component2
// test.kt:25 box
// test.kt:10 copy
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:26 box

// EXPECTATIONS FIR JS_IR // TODO: There is an inconsistency in names between K1 and K2. This should be fixed in KT-64435.
// test.kt:14 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:16 box
// test.kt:1 protoOf.hashCode
// test.kt:1 protoOf.hashCode
// test.kt:17 box
// test.kt:1 protoOf.toString
// test.kt:18 box
// test.kt:1 protoOf.component1_7eebsc_k$
// test.kt:18 box
// test.kt:1 protoOf.component2_7eebsb_k$
// test.kt:19 box
// test.kt:1 protoOf.copy$default_8mg6yi_k$
// test.kt:1 protoOf.copy$default_8mg6yi_k$
// test.kt:1 protoOf.copy_xhhsuv_k$
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:20 box
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:21 box
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:21 box
// test.kt:8 equals
// test.kt:22 box
// test.kt:9 hashCode
// test.kt:23 box
// test.kt:7 toString
// test.kt:24 box
// test.kt:1 protoOf.component1_7eebsc_k$
// test.kt:24 box
// test.kt:1 protoOf.component2_7eebsb_k$
// test.kt:25 box
// test.kt:10 copy
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:26 box

// EXPECTATIONS WASM
// test.kt:14 $box (12, 14, 17, 17, 17, 17, 12)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set (5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
// String.kt:149 $kotlin.stringLiteral (11, 4, 11, 4, 11, 4, 11, 4, 11, 4, 11, 4, 11, 4, 11, 4, 11, 4, 11, 4)
// test.kt:4 $D.<init> (13, 25, 39, 13, 25, 39, 13, 25, 39)
// test.kt:15 $box (4, 13, 15, 18, 18, 18, 18, 13, 6, 6)
// String.kt:143 $kotlin.stringLiteral (15, 8, 15, 8, 15, 8)
// String.kt:98 $kotlin.String.equals
// String.kt:99 $kotlin.String.equals (12, 35, 28)
// test.kt:16 $box (4, 6, 6)
// Primitives.kt:1366 $kotlin.Int__hashCode-impl
// String.kt:122 $kotlin.String.hashCode (12, 25, 12, 12)
// String.kt:123 $kotlin.String.hashCode
// String.kt:124 $kotlin.String.hashCode (12, 26, 12)
// String.kt:126 $kotlin.String.hashCode (24, 8)
// String.kt:63 $kotlin.String.hashCode
// String.kt:66 $kotlin.String.hashCode (15, 8)
// String.kt:127 $kotlin.String.hashCode (19, 8)
// String.kt:128 $kotlin.String.hashCode (8, 8)
// Standard.kt:136 $kotlin.String.hashCode (18, 52, 61, 52, 66, 51, 51, 44)
// Standard.kt:152 $kotlin.String.hashCode
// Standard.kt:154 $kotlin.String.hashCode (18, 4, 26, 4, 18, 9, 18, 4, 26, 4, 4)
// Standard.kt:155 $kotlin.String.hashCode (8, 15)
// Standard.kt:138 $kotlin.String.hashCode (8, 4, 0)
// Standard.kt:137 $kotlin.String.hashCode
// Standard.kt:53 $kotlin.String.hashCode (25, 37)
// String.kt:131 $kotlin.String.hashCode (20, 8)
// String.kt:132 $kotlin.String.hashCode (15, 8)
// test.kt:17 $box (4, 6, 6)
// StringBuilder.kt:17 $kotlin.text.StringBuilder.<init> (39, 34, 34, 42)
// StringBuilder.kt:20 $kotlin.text.StringBuilder.<init> (52, 62, 52, 47, 47, 72)
// Arrays.kt:95 $kotlin.CharArray.<init> (12, 19, 12, 12, 19, 12)
// Arrays.kt:96 $kotlin.CharArray.<init> (8, 32, 18, 8, 8, 32, 18, 8)
// Arrays.kt:139 $kotlin.CharArray.<init> (1, 1)
// StringBuilder.kt:14 $kotlin.text.StringBuilder.<init>
// StringBuilder.kt:33 $kotlin.text.StringBuilder.<init>
// StringBuilder.kt:715 $kotlin.text.StringBuilder.<init>
// StringBuilder.kt:143 $kotlin.text.StringBuilder.append (59, 66, 72, 59, 83, 59, 66, 72, 59, 83, 59, 66, 72, 59, 83, 59, 66, 72, 59, 83)
// Library.kt:19 $kotlin.toString (37, 37, 43, 43, 43, 37, 37, 63, 37, 37, 43, 43, 43, 37, 37, 63, 37, 37, 43, 43, 43, 37, 37, 63, 37, 37, 43, 43, 43, 37, 37, 63)
// String.kt:119 $kotlin.String.toString (49, 49, 49, 49)
// StringBuilder.kt:225 $kotlin.text.StringBuilder.append (23, 23, 8, 23, 23, 8, 23, 23, 8, 23, 23, 8)
// StringBuilder.kt:226 $kotlin.text.StringBuilder.append (8, 28, 37, 8, 8, 28, 37, 8, 8, 28, 37, 8, 8, 28, 37, 8)
// StringBuilder.kt:696 $kotlin.text.StringBuilder.ensureExtraCapacity (8, 31, 41, 31, 8, 8, 31, 41, 31, 8, 8, 31, 41, 31, 8, 8, 31, 41, 31, 8, 8, 31, 41, 31, 8)
// StringBuilder.kt:700 $kotlin.text.StringBuilder.ensureCapacityInternal (12, 26, 12, 12, 26, 12, 12, 26, 12, 12, 26, 12, 12, 26, 12)
// StringBuilder.kt:701 $kotlin.text.StringBuilder.ensureCapacityInternal (12, 26, 32, 12, 12, 26, 32, 12, 12, 26, 32, 12, 12, 26, 32, 12, 12, 26, 32, 12)
// Arrays.kt:135 $kotlin.CharArray.<get-size> (16, 24, 29, 16, 24, 29, 16, 24, 29, 16, 24, 29, 16, 24, 29, 16, 24, 29, 16, 24, 29, 16, 24, 29, 16, 24, 29)
// StringBuilder.kt:705 $kotlin.text.StringBuilder.ensureCapacityInternal (5, 5, 5, 5, 5)
// StringBuilder.kt:697 $kotlin.text.StringBuilder.ensureExtraCapacity (5, 5, 5, 5, 5)
// StringBuilder.kt:227 $kotlin.text.StringBuilder.append (8, 32, 39, 48, 19, 8, 8, 8, 32, 39, 48, 19, 8, 8, 8, 32, 39, 48, 19, 8, 8, 8, 32, 39, 48, 19, 8, 8)
// StringBuilder.kt:915 $kotlin.text.insertString (21, 28, 35, 42, 45, 51, 8, 58, 21, 28, 35, 42, 45, 51, 8, 58, 21, 28, 35, 42, 45, 51, 8, 58, 21, 28, 35, 42, 45, 51, 8, 58)
// StringBuilderWasm.kt:41 $kotlin.text.insertString (4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// _WasmArrays.kt:62 $kotlin.text.insertString (53, 47, 60, 66, 53, 47, 60, 66, 53, 47, 60, 66, 53, 47, 60, 66, 53, 47, 60, 66)
// _WasmArrays.kt:79 $kotlin.text.insertString (21, 21, 21, 21, 21)
// _WasmArrays.kt:83 $kotlin.text.insertString (10, 3, 10, 3, 10, 3, 10, 3, 10, 3)
// _WasmArrays.kt:63 $kotlin.text.insertString (2, 2, 2, 2, 2)
// _WasmArrays.kt:64 $kotlin.text.insertString (1, 19, 1, 19, 1, 19, 1, 19, 1, 19)
// _WasmArrays.kt:88 $kotlin.text.insertString (35, 48, 66, 74, 87, 4, 35, 48, 66, 74, 87, 4, 35, 48, 66, 74, 87, 4, 35, 48, 66, 74, 87, 4, 35, 48, 66, 74, 87, 4)
// StringBuilderWasm.kt:42 $kotlin.text.insertString (11, 4, 11, 4, 11, 4, 11, 4, 11, 4)
// StringBuilder.kt:228 $kotlin.text.StringBuilder.append (15, 8, 15, 8, 15, 8, 15, 8)
// StringBuilder.kt:178 $kotlin.text.StringBuilder.append (8, 28, 8)
// StringBuilder.kt:702 $kotlin.text.StringBuilder.ensureCapacityInternal (51, 57, 63, 39, 12)
// AbstractList.kt:141 $kotlin.collections.Companion.newCapacity (30, 45, 61, 45, 30)
// AbstractList.kt:142 $kotlin.collections.Companion.newCapacity (16, 30, 16, 44, 16)
// AbstractList.kt:144 $kotlin.collections.Companion.newCapacity (16, 30, 16, 45, 16)
// AbstractList.kt:146 $kotlin.collections.Companion.newCapacity (19, 12)
// StringBuilder.kt:703 $kotlin.text.StringBuilder.ensureCapacityInternal (12, 20, 33, 26, 12)
// _ArraysWasm.kt:1417 $kotlin.collections.copyOf (11, 44, 16, 4)
// _ArraysWasm.kt:1781 $kotlin.collections.copyOfUninitializedElements (11, 39, 42, 11, 4)
// _ArraysWasm.kt:1694 $kotlin.collections.copyOfUninitializedElements (18, 28, 18)
// _ArraysWasm.kt:1695 $kotlin.collections.copyOfUninitializedElements (8, 18, 8)
// _ArraysWasm.kt:1698 $kotlin.collections.copyOfUninitializedElements (17, 27, 17, 4)
// _ArraysWasm.kt:1699 $kotlin.collections.copyOfUninitializedElements (4, 18, 26, 29, 40, 61, 48, 9, 9)
// _Ranges.kt:1321 $kotlin.ranges.coerceAtMost (15, 22, 15, 36, 4)
// _ArraysWasm.kt:1228 $kotlin.collections.copyInto (35, 47, 57, 62, 17)
// AbstractList.kt:119 $kotlin.collections.Companion.checkRangeIndexes (16, 28, 16, 33, 43, 33, 16, 28, 16, 33, 43, 33)
// AbstractList.kt:122 $kotlin.collections.Companion.checkRangeIndexes (16, 28, 16, 16, 28, 16)
// AbstractList.kt:125 $kotlin.collections.Companion.checkRangeIndexes (9, 9)
// _ArraysWasm.kt:1229 $kotlin.collections.copyInto (20, 31, 20, 4)
// _ArraysWasm.kt:1230 $kotlin.collections.copyInto (35, 54, 74, 54, 85, 97, 17)
// _ArraysWasm.kt:1231 $kotlin.collections.copyInto (25, 25)
// _WasmArrays.kt:244 $kotlin.collections.copyInto (42869, 42874, 42883, 42895, 42904, 42916, 42935)
// _WasmArrays.kt:88 $kotlin.collections.copyInto (35, 48, 66, 74, 87, 4)
// _ArraysWasm.kt:1232 $kotlin.collections.copyInto (11, 4)
// _ArraysWasm.kt:1700 $kotlin.collections.copyOfUninitializedElements (11, 4)
// StringBuilder.kt:179 $kotlin.text.StringBuilder.append (8, 29, 36, 45, 19, 8, 8)
// StringBuilderWasm.kt:52 $kotlin.text.insertInt (22, 28)
// Primitives.kt:1359 $kotlin.Int__toString-impl (8, 20)
// Number2String.kt:191 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:192 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:193 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:194 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:195 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:196 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:198 $kotlin.wasm.internal.<init properties Number2String.kt>
// Library.kt:93 $kotlin.wasm.internal.<init properties Number2String.kt> (2619, 2619, 2619, 2619, 3210, 3210, 3210, 3210, 3231, 3210, 11556, 11560, 3262, 3241, 11556, 11560, 3293, 3272, 11556, 11560, 3324, 3303, 11556, 11560, 3359, 3338, 11556, 11560, 3390, 3369, 11556, 11560, 3421, 3400, 11556, 11560, 3452, 3431, 11556, 11560, 3487, 3466, 11556, 11560, 3518, 3497, 11556, 11560, 3549, 3528, 11556, 11560, 3580, 3559, 11556, 11560, 3615, 3594, 11556, 11560, 3646, 3625, 11556, 11560, 3677, 3656, 11556, 11560, 3708, 3687, 11556, 11560, 3743, 3722, 11556, 11560, 3774, 3753, 11556, 11560, 3805, 3784, 11556, 11560, 3836, 3815, 11556, 11560, 3871, 3850, 11556, 11560, 3902, 3881, 11556, 11560, 3933, 3912, 11556, 11560, 3964, 3943, 11556, 11560, 3999, 3978, 11556, 11560, 4030, 4009, 11556, 11560, 4061, 4040, 11556, 11560, 4092, 4071, 11556, 11560, 4127, 4106, 11556, 11560, 4158, 4137, 11556, 11560, 4189, 4168, 11556, 11560, 4220, 4199, 11556, 11560, 4255, 4234, 11556, 11560, 4286, 4265, 11556, 11560, 4317, 4296, 11556, 11560, 4348, 4327, 11556, 11560, 4383, 4362, 11556, 11560, 4414, 4393, 11556, 11560, 4445, 4424, 11556, 11560, 4476, 4455, 11556, 11560, 4511, 4490, 11556, 11560, 4542, 4521, 11556, 11560, 4573, 4552, 11556, 11560, 4604, 4583, 11556, 11560, 4639, 4618, 11556, 11560, 4670, 4649, 11556, 11560, 4701, 4680, 11556, 11560, 4732, 4711, 11556, 11560, 4767, 4746, 11556, 11560, 4798, 4777, 11556, 11560, 4829, 4808, 11556, 11560, 4860, 4839, 11556, 11560, 4895, 4874, 11556, 11560, 4926, 4905, 11556, 11560, 4957, 4936, 11556, 11560, 4988, 4967, 11556, 11560, 5023, 5002, 11556, 11560, 5054, 5033, 11556, 11560, 5085, 5064, 11556, 11560, 5116, 5095, 11556, 11560, 5151, 5130, 11556, 11560, 5182, 5161, 11556, 11560, 5213, 5192, 11556, 11560, 5244, 5223, 11556, 11560, 5279, 5258, 11556, 11560, 5310, 5289, 11556, 11560, 5341, 5320, 11556, 11560, 5372, 5351, 11556, 11560, 5407, 5386, 11556, 11560, 5438, 5417, 11556, 11560, 5469, 5448, 11556, 11560, 5500, 5479, 11556, 11560, 5535, 5514, 11556, 11560, 5566, 5545, 11556, 11560, 5597, 5576, 11556, 11560, 5628, 5607, 11556, 11560, 5663, 5642, 11556, 11560, 5694, 5673, 11556, 11560, 5725, 5704, 11556, 11560, 5756, 5735, 11556, 11560, 5791, 5770, 11556, 11560, 5822, 5801, 11556, 11560, 5853, 5832, 11556, 11560, 5884, 5863, 11556, 11560, 5919, 5898, 11556, 11560, 5950, 5929, 11556, 11560, 5981, 5960, 11556, 11560, 3210, 3210)
// Library.kt:69 $kotlin.wasm.internal.<init properties Number2String.kt> (69, 77)
// Number2String.kt:211 $kotlin.wasm.internal.<init properties Number2String.kt>
// ULong.kt:17 $kotlin.<ULong__<get-data>-impl> (125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125)
// Library.kt:54 $kotlin.wasm.internal.<init properties Number2String.kt> (66, 74)
// Number2String.kt:48 $kotlin.wasm.internal.itoa32 (8, 22, 8)
// Number2String.kt:50 $kotlin.wasm.internal.itoa32 (21, 34, 21)
// Number2String.kt:51 $kotlin.wasm.internal.itoa32 (23, 52, 4)
// Number2String.kt:52 $kotlin.wasm.internal.itoa32 (41, 25, 4)
// UInt.kt:36 $kotlin.wasm.internal.itoa32
// UInt.kt:414 $kotlin.wasm.internal.itoa32 (44, 39, 49)
// UInt.kt:17 $kotlin.<UInt__<init>-impl>
// Number2String.kt:58 $kotlin.wasm.internal.utoa32 (8, 22, 8)
// Number2String.kt:60 $kotlin.wasm.internal.utoa32 (34, 19)
// Number2String.kt:105 $kotlin.wasm.internal.decimalCount32 (8, 8, 8)
// UInt.kt:68 $kotlin.wasm.internal.decimalCount32 (4, 12, 35, 43, 75, 84)
// UInt.kt:64 $kotlin.wasm.internal.decimalCount32 (70, 82, 87, 93, 99, 104, 70, 82, 87, 93, 99, 104, 70, 82, 87, 93, 99, 104)
// UInt.kt:17 $kotlin.<UInt__<get-data>-impl> (124, 124, 124, 124, 124, 124, 124, 124, 124)
// UInt.kt:31 $kotlin.wasm.internal.decimalCount32 (65, 69, 46, 72, 65, 69, 46, 72, 65, 69, 46, 72)
// WasmMath.kt:16 $kotlin.wasm.internal.wasm_u32_compareTo (18, 21, 4, 48, 51, 34, 4, 61, 18, 21, 4, 48, 51, 34, 4, 61, 18, 21, 4, 48, 51, 34, 4, 61, 18, 21, 4, 48, 51, 34, 4, 61)
// Number2String.kt:106 $kotlin.wasm.internal.decimalCount32 (12, 12, 12)
// Number2String.kt:107 $kotlin.wasm.internal.decimalCount32 (19, 24, 24, 24, 19, 12)
// Number2String.kt:61 $kotlin.wasm.internal.utoa32 (28, 14)
// Number2String.kt:63 $kotlin.wasm.internal.utoa32 (18, 23, 35, 4)
// Number2String.kt:69 $kotlin.wasm.internal.utoaDecSimple (11, 23, 11, 11, 4)
// Assertions.kt:14 $kotlin.assert (11, 4, 11, 4, 11, 4, 11, 4)
// Assertions.kt:21 $kotlin.assert (9, 8, 9, 8, 9, 8, 9, 8)
// Assertions.kt:25 $kotlin.assert (1, 1, 1, 1)
// Assertions.kt:15 $kotlin.assert (1, 1, 1, 1)
// Number2String.kt:70 $kotlin.wasm.internal.utoaDecSimple (11, 18, 26, 11, 4)
// Number2String.kt:71 $kotlin.wasm.internal.utoaDecSimple (11, 25, 11, 30, 45, 52, 30, 4)
// Number2String.kt:73 $kotlin.wasm.internal.utoaDecSimple (14, 4)
// Number2String.kt:74 $kotlin.wasm.internal.utoaDecSimple (17, 4)
// Number2String.kt:76 $kotlin.wasm.internal.utoaDecSimple (16, 8)
// UInt.kt:51 $kotlin.wasm.internal.utoaDecSimple (75, 81, 101, 107)
// UInt.kt:121 $kotlin.wasm.internal.utoaDecSimple (67, 73, 56, 79)
// Number2String.kt:77 $kotlin.wasm.internal.utoaDecSimple (16, 8)
// UInt.kt:146 $kotlin.wasm.internal.utoaDecSimple (70, 76, 56, 82)
// Number2String.kt:78 $kotlin.wasm.internal.utoaDecSimple (14, 8)
// Number2String.kt:79 $kotlin.wasm.internal.utoaDecSimple
// Primitives.kt:1159 $kotlin.Int__dec-impl (15, 8, 16)
// Number2String.kt:80 $kotlin.wasm.internal.utoaDecSimple (8, 19, 41, 27, 15)
// UInt.kt:54 $kotlin.wasm.internal.utoaDecSimple (3, 28)
// UInt.kt:313 $kotlin.wasm.internal.utoaDecSimple (37, 41)
// Number2String.kt:43 $kotlin.wasm.internal.digitToChar (20, 11, 23, 11, 4)
// Number2String.kt:11 $kotlin.wasm.internal.CharCodes_initEntries
// Enum.kt:9 $kotlin.Enum.<init> (4, 4, 4, 4, 4)
// Enum.kt:11 $kotlin.Enum.<init> (4, 4, 4, 4, 4)
// Enum.kt:27 $kotlin.Enum.<init> (1, 1, 1, 1, 1)
// Number2String.kt:9 $kotlin.wasm.internal.CharCodes.<init> (29, 29, 29, 29, 29)
// Number2String.kt:40 $kotlin.wasm.internal.CharCodes.<init> (1, 1, 1, 1, 1)
// Number2String.kt:12 $kotlin.wasm.internal.CharCodes_initEntries
// Number2String.kt:13 $kotlin.wasm.internal.CharCodes_initEntries
// Number2String.kt:14 $kotlin.wasm.internal.CharCodes_initEntries
// Number2String.kt:34 $kotlin.wasm.internal.CharCodes_initEntries
// Number2String.kt:44 $kotlin.wasm.internal.digitToChar (25, 32, 12, 39, 4)
// Primitives.kt:1306 $kotlin.Int__toChar-impl (18, 9, 45)
// Number2String.kt:81 $kotlin.wasm.internal.utoaDecSimple (13, 13, 13, 13)
// UInt.kt:55 $kotlin.wasm.internal.utoaDecSimple
// UInt.kt:64 $kotlin.wasm.internal.utoaDecSimple (70, 82, 87, 93, 99, 104)
// UInt.kt:31 $kotlin.wasm.internal.utoaDecSimple (65, 69, 46, 72)
// Number2String.kt:82 $kotlin.wasm.internal.utoaDecSimple
// Number2String.kt:65 $kotlin.wasm.internal.utoa32 (15, 4)
// String.kt:44 $kotlin.wasm.internal.utoa32
// String.kt:138 $kotlin.wasm.internal.utoa32 (4, 4, 4, 4, 11, 17, 22, 29, 4, 34)
// Number2String.kt:54 $kotlin.wasm.internal.itoa32 (15, 51, 4)
// StringBuilderWasm.kt:53 $kotlin.text.insertInt (17, 29, 4)
// StringBuilderWasm.kt:54 $kotlin.text.insertInt (17, 24, 31, 44, 47, 4, 4)
// StringBuilderWasm.kt:55 $kotlin.text.insertInt (11, 4)
// StringBuilder.kt:180 $kotlin.text.StringBuilder.append (15, 8)
// StringBuilder.kt:499 $kotlin.text.StringBuilder.toString (64, 71, 74, 38, 82)
// StringBuilderWasm.kt:46 $kotlin.text.unsafeStringFromCharArray (29, 15, 4)
// StringBuilderWasm.kt:47 $kotlin.text.unsafeStringFromCharArray (4, 4)
// _WasmArrays.kt:73 $kotlin.text.unsafeStringFromCharArray (3, 9, 18, 24, 31)
// _WasmArrays.kt:74 $kotlin.text.unsafeStringFromCharArray
// _WasmArrays.kt:88 $kotlin.text.unsafeStringFromCharArray (35, 48, 66, 74, 87, 4)
// StringBuilderWasm.kt:48 $kotlin.text.unsafeStringFromCharArray (16, 4)
// String.kt:57 $kotlin.text.unsafeStringFromCharArray
// String.kt:138 $kotlin.text.unsafeStringFromCharArray (4, 4, 4, 4, 11, 17, 22, 29, 4, 34)
// test.kt:18 $box (17, 9, 9, 17, 12, 12)
// test.kt:1 $D.component1
// test.kt:19 $box (4, 6, 6, 6, 6, 6, 6)
// test.kt:20 $box (12, 14, 17, 17, 17, 17, 12)
// test.kt:6 $E.<init> (13, 25, 13, 25, 13, 25)
// test.kt:11 $E.<init> (1, 1, 1)
// test.kt:21 $box (4, 13, 15, 18, 18, 18, 18, 13, 6, 6)
// test.kt:8 $E.equals (39, 44)
// test.kt:22 $box (4, 6, 6)
// test.kt:9 $E.hashCode (30, 32)
// test.kt:23 $box (4, 6, 6)
// test.kt:7 $E.toString (30, 30, 30, 30, 34)
// test.kt:24 $box (19, 9, 9, 19, 13, 13)
// test.kt:1 $E.component1
// test.kt:25 $box (4, 6, 6)
// test.kt:10 $E.copy (17, 19, 22, 17, 24)
// test.kt:26 $box
