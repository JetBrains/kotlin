
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
// Primitives.kt:1359 $kotlin.Int__toString-impl (21, 8, 24)
// Number2String.kt:199 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:200 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:201 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:202 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:203 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:204 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:206 $kotlin.wasm.internal.<init properties Number2String.kt>
// Library.kt:93 $kotlin.wasm.internal.<init properties Number2String.kt> (2841, 2841, 2841, 2841, 3432, 3432, 3432, 3432, 3453, 3432, 11556, 11560, 3484, 3463, 11556, 11560, 3515, 3494, 11556, 11560, 3546, 3525, 11556, 11560, 3581, 3560, 11556, 11560, 3612, 3591, 11556, 11560, 3643, 3622, 11556, 11560, 3674, 3653, 11556, 11560, 3709, 3688, 11556, 11560, 3740, 3719, 11556, 11560, 3771, 3750, 11556, 11560, 3802, 3781, 11556, 11560, 3837, 3816, 11556, 11560, 3868, 3847, 11556, 11560, 3899, 3878, 11556, 11560, 3930, 3909, 11556, 11560, 3965, 3944, 11556, 11560, 3996, 3975, 11556, 11560, 4027, 4006, 11556, 11560, 4058, 4037, 11556, 11560, 4093, 4072, 11556, 11560, 4124, 4103, 11556, 11560, 4155, 4134, 11556, 11560, 4186, 4165, 11556, 11560, 4221, 4200, 11556, 11560, 4252, 4231, 11556, 11560, 4283, 4262, 11556, 11560, 4314, 4293, 11556, 11560, 4349, 4328, 11556, 11560, 4380, 4359, 11556, 11560, 4411, 4390, 11556, 11560, 4442, 4421, 11556, 11560, 4477, 4456, 11556, 11560, 4508, 4487, 11556, 11560, 4539, 4518, 11556, 11560, 4570, 4549, 11556, 11560, 4605, 4584, 11556, 11560, 4636, 4615, 11556, 11560, 4667, 4646, 11556, 11560, 4698, 4677, 11556, 11560, 4733, 4712, 11556, 11560, 4764, 4743, 11556, 11560, 4795, 4774, 11556, 11560, 4826, 4805, 11556, 11560, 4861, 4840, 11556, 11560, 4892, 4871, 11556, 11560, 4923, 4902, 11556, 11560, 4954, 4933, 11556, 11560, 4989, 4968, 11556, 11560, 5020, 4999, 11556, 11560, 5051, 5030, 11556, 11560, 5082, 5061, 11556, 11560, 5117, 5096, 11556, 11560, 5148, 5127, 11556, 11560, 5179, 5158, 11556, 11560, 5210, 5189, 11556, 11560, 5245, 5224, 11556, 11560, 5276, 5255, 11556, 11560, 5307, 5286, 11556, 11560, 5338, 5317, 11556, 11560, 5373, 5352, 11556, 11560, 5404, 5383, 11556, 11560, 5435, 5414, 11556, 11560, 5466, 5445, 11556, 11560, 5501, 5480, 11556, 11560, 5532, 5511, 11556, 11560, 5563, 5542, 11556, 11560, 5594, 5573, 11556, 11560, 5629, 5608, 11556, 11560, 5660, 5639, 11556, 11560, 5691, 5670, 11556, 11560, 5722, 5701, 11556, 11560, 5757, 5736, 11556, 11560, 5788, 5767, 11556, 11560, 5819, 5798, 11556, 11560, 5850, 5829, 11556, 11560, 5885, 5864, 11556, 11560, 5916, 5895, 11556, 11560, 5947, 5926, 11556, 11560, 5978, 5957, 11556, 11560, 6013, 5992, 11556, 11560, 6044, 6023, 11556, 11560, 6075, 6054, 11556, 11560, 6106, 6085, 11556, 11560, 6141, 6120, 11556, 11560, 6172, 6151, 11556, 11560, 6203, 6182, 11556, 11560, 3432, 3432)
// Library.kt:69 $kotlin.wasm.internal.<init properties Number2String.kt>
// Number2String.kt:219 $kotlin.wasm.internal.<init properties Number2String.kt>
// ULong.kt:17 $kotlin.<ULong__<get-data>-impl> (125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125, 125)
// Library.kt:54 $kotlin.wasm.internal.<init properties Number2String.kt> (66, 74)
// Number2String.kt:47 $kotlin.wasm.internal.itoa32 (8, 16, 8, 21, 29, 21)
// Number2String.kt:50 $kotlin.wasm.internal.itoa32 (8, 17, 8, 8)
// Number2String.kt:53 $kotlin.wasm.internal.itoa32 (8, 22, 8)
// Number2String.kt:55 $kotlin.wasm.internal.itoa32 (8, 26, 8)
// Number2String.kt:57 $kotlin.wasm.internal.itoa32 (15, 31, 15)
// Number2String.kt:58 $kotlin.wasm.internal.itoa32 (11, 19, 11, 24, 32, 24, 4)
// Assertions.kt:14 $kotlin.assert (11, 18, 18, 4, 11, 18, 18, 4, 11, 18, 18, 4, 11, 18, 18, 4, 11, 18, 18, 4, 11, 18, 18, 4)
// Assertions.kt:21 $kotlin.assert (9, 8, 9, 8, 9, 8, 9, 8, 9, 8, 9, 8)
// Assertions.kt:25 $kotlin.assert (1, 1, 1, 1, 1, 1)
// Assertions.kt:15 $kotlin.assert (1, 1, 1, 1, 1, 1)
// Number2String.kt:59 $kotlin.wasm.internal.itoa32 (23, 31, 23, 51)
// Number2String.kt:61 $kotlin.wasm.internal.itoa32 (34, 19, 46, 19)
// Number2String.kt:107 $kotlin.wasm.internal.decimalCount32 (8, 16, 8)
// Number2String.kt:108 $kotlin.wasm.internal.decimalCount32 (12, 20, 12)
// Number2String.kt:109 $kotlin.wasm.internal.decimalCount32 (19, 24, 33, 24, 19, 12)
// Number2String.kt:62 $kotlin.wasm.internal.itoa32 (28, 14)
// Number2String.kt:63 $kotlin.wasm.internal.itoa32 (18, 23, 33, 4)
// Number2String.kt:71 $kotlin.wasm.internal.utoaDecSimple (11, 23, 11, 11, 4)
// Number2String.kt:72 $kotlin.wasm.internal.utoaDecSimple (11, 18, 26, 11, 4)
// Number2String.kt:73 $kotlin.wasm.internal.utoaDecSimple (11, 25, 11, 30, 45, 52, 30, 4)
// Number2String.kt:75 $kotlin.wasm.internal.utoaDecSimple (14, 4)
// Number2String.kt:76 $kotlin.wasm.internal.utoaDecSimple (17, 4)
// Number2String.kt:78 $kotlin.wasm.internal.utoaDecSimple (16, 22, 16, 8, 16, 22, 16, 8)
// Primitives.kt:1066 $kotlin.Int__div-impl (24, 12, 69, 96, 24, 12, 69, 96)
// Number2String.kt:79 $kotlin.wasm.internal.utoaDecSimple (16, 22, 16, 8, 16, 22, 16, 8)
// Number2String.kt:80 $kotlin.wasm.internal.utoaDecSimple (14, 8, 14, 8)
// Number2String.kt:81 $kotlin.wasm.internal.utoaDecSimple (8, 8)
// Primitives.kt:1159 $kotlin.Int__dec-impl (15, 8, 16, 15, 8, 16)
// Number2String.kt:82 $kotlin.wasm.internal.utoaDecSimple (8, 19, 39, 27, 15, 8, 19, 39, 27, 15)
// Number2String.kt:41 $kotlin.wasm.internal.digitToChar (20, 11, 23, 11, 4, 20, 11, 23, 11, 4)
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
// Number2String.kt:9 $kotlin.wasm.internal.CharCodes_initEntries
// Enum.kt:9 $kotlin.Enum.<init> (4, 4, 4, 4, 4)
// Enum.kt:11 $kotlin.Enum.<init> (4, 4, 4, 4, 4)
// Enum.kt:27 $kotlin.Enum.<init> (1, 1, 1, 1, 1)
// Number2String.kt:7 $kotlin.wasm.internal.CharCodes.<init> (29, 29, 29, 29, 29)
// Number2String.kt:38 $kotlin.wasm.internal.CharCodes.<init> (1, 1, 1, 1, 1)
// Number2String.kt:10 $kotlin.wasm.internal.CharCodes_initEntries
// Number2String.kt:11 $kotlin.wasm.internal.CharCodes_initEntries
// Number2String.kt:12 $kotlin.wasm.internal.CharCodes_initEntries
// Number2String.kt:32 $kotlin.wasm.internal.CharCodes_initEntries
// Number2String.kt:42 $kotlin.wasm.internal.digitToChar (25, 32, 12, 39, 4, 25, 32, 12, 39, 4)
// Primitives.kt:1306 $kotlin.Int__toChar-impl (18, 9, 45, 18, 9, 45)
// Number2String.kt:83 $kotlin.wasm.internal.utoaDecSimple (13, 19, 13, 13, 13, 19, 13, 13)
// Number2String.kt:84 $kotlin.wasm.internal.utoaDecSimple
// Number2String.kt:64 $kotlin.wasm.internal.itoa32 (8, 16, 8)
// Number2String.kt:67 $kotlin.wasm.internal.itoa32 (15, 4)
// String.kt:138 $kotlin.wasm.internal.itoa32 (4, 4, 4, 4, 11, 17, 22, 29, 4, 34)
// test.kt:13 $box
