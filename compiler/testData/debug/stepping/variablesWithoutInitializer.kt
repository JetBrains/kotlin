
// FILE: test.kt

fun box() {
    var x: String
    var y: Int
    var z: Boolean
    z = false
    y = 42
    if (!z) {
        x = y.toString()
    }
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:11 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:11 box
// test.kt:13 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:8 $box (8, 4)
// test.kt:9 $box (8, 4)
// test.kt:10 $box (9, 8)
// test.kt:11 $box (12, 14, 8)
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
// UInt.kt:17 $kotlin.<UInt__<get-data>-impl> (124, 124, 124, 124, 124, 124, 124, 124, 124, 124, 124, 124)
// UInt.kt:31 $kotlin.wasm.internal.decimalCount32 (65, 69, 46, 72, 65, 69, 46, 72, 65, 69, 46, 72)
// WasmMath.kt:16 $kotlin.wasm.internal.wasm_u32_compareTo (18, 21, 4, 48, 51, 34, 4, 61, 18, 21, 4, 48, 51, 34, 4, 61, 18, 21, 4, 48, 51, 34, 4, 61, 18, 21, 4, 48, 51, 34, 4, 61, 18, 21, 4, 48, 51, 34, 4, 61)
// Number2String.kt:106 $kotlin.wasm.internal.decimalCount32 (12, 12, 12)
// Number2String.kt:107 $kotlin.wasm.internal.decimalCount32 (19, 24, 24, 24, 19, 12)
// Number2String.kt:61 $kotlin.wasm.internal.utoa32 (28, 14)
// Number2String.kt:63 $kotlin.wasm.internal.utoa32 (18, 23, 35, 4)
// Number2String.kt:69 $kotlin.wasm.internal.utoaDecSimple (11, 23, 11, 11, 4)
// Assertions.kt:14 $kotlin.assert (11, 4, 11, 4, 11, 4, 11, 4, 11, 4)
// Assertions.kt:21 $kotlin.assert (9, 8, 9, 8, 9, 8, 9, 8, 9, 8)
// Assertions.kt:25 $kotlin.assert (1, 1, 1, 1, 1)
// Assertions.kt:15 $kotlin.assert (1, 1, 1, 1, 1)
// Number2String.kt:70 $kotlin.wasm.internal.utoaDecSimple (11, 18, 26, 11, 4)
// Number2String.kt:71 $kotlin.wasm.internal.utoaDecSimple (11, 25, 11, 30, 45, 52, 30, 4)
// Number2String.kt:73 $kotlin.wasm.internal.utoaDecSimple (14, 4)
// Number2String.kt:74 $kotlin.wasm.internal.utoaDecSimple (17, 4)
// Number2String.kt:76 $kotlin.wasm.internal.utoaDecSimple (16, 8, 16, 8)
// UInt.kt:51 $kotlin.wasm.internal.utoaDecSimple (75, 81, 101, 107, 75, 81, 101, 107)
// UInt.kt:121 $kotlin.wasm.internal.utoaDecSimple (67, 73, 56, 79, 67, 73, 56, 79)
// Number2String.kt:77 $kotlin.wasm.internal.utoaDecSimple (16, 8, 16, 8)
// UInt.kt:146 $kotlin.wasm.internal.utoaDecSimple (70, 76, 56, 82, 70, 76, 56, 82)
// Number2String.kt:78 $kotlin.wasm.internal.utoaDecSimple (14, 8, 14, 8)
// Number2String.kt:79 $kotlin.wasm.internal.utoaDecSimple (8, 8)
// Primitives.kt:1159 $kotlin.Int__dec-impl (15, 8, 16, 15, 8, 16)
// Number2String.kt:80 $kotlin.wasm.internal.utoaDecSimple (8, 19, 41, 27, 15, 8, 19, 41, 27, 15)
// UInt.kt:54 $kotlin.wasm.internal.utoaDecSimple (3, 28, 3, 28)
// UInt.kt:313 $kotlin.wasm.internal.utoaDecSimple (37, 41, 37, 41)
// Number2String.kt:43 $kotlin.wasm.internal.digitToChar (20, 11, 23, 11, 4, 20, 11, 23, 11, 4)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral (8, 8, 8, 8, 8)
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set (5, 5, 5, 5, 5)
// String.kt:149 $kotlin.stringLiteral (11, 4, 11, 4, 11, 4, 11, 4, 11, 4)
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
// Number2String.kt:44 $kotlin.wasm.internal.digitToChar (25, 32, 12, 39, 4, 25, 32, 12, 39, 4)
// Primitives.kt:1306 $kotlin.Int__toChar-impl (18, 9, 45, 18, 9, 45)
// Number2String.kt:81 $kotlin.wasm.internal.utoaDecSimple (13, 13, 13, 13, 13, 13, 13, 13)
// UInt.kt:55 $kotlin.wasm.internal.utoaDecSimple (2, 2)
// UInt.kt:64 $kotlin.wasm.internal.utoaDecSimple (70, 82, 87, 93, 99, 104, 70, 82, 87, 93, 99, 104)
// UInt.kt:31 $kotlin.wasm.internal.utoaDecSimple (65, 69, 46, 72, 65, 69, 46, 72)
// Number2String.kt:82 $kotlin.wasm.internal.utoaDecSimple
// Number2String.kt:65 $kotlin.wasm.internal.utoa32 (15, 4)
// String.kt:44 $kotlin.wasm.internal.utoa32
// String.kt:138 $kotlin.wasm.internal.utoa32 (4, 4, 4, 4, 11, 17, 22, 29, 4, 34)
// Number2String.kt:54 $kotlin.wasm.internal.itoa32 (15, 51, 4)
// test.kt:13 $box
