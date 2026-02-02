// MODULE: library
// FILE: library.kt

inline fun foo() {
    val fooVar = 100
    x1(1)
    x4(4)
}

inline fun x1(x1Var: Int) {
    val y1 = 1
    x2(2)
    x3(3)
}

inline fun x2(x2Var: Int) {
    val y2 = 2
}

inline fun x3(x3Var: Int) {
    val y3 = 3
}

inline fun x4(x4Var: Int) {
    val y4 = 4
    x5(5)
    x6(6)
}

inline fun x5(x5Var: Int) {
    val y5 = 5
}

inline fun x6(x6Var: Int) {
    val y6 = 6
}

// MODULE: test(library)
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    val m = 1
    foo()
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:43 box:
// test.kt:44 box: m:int=1:int
// library.kt:5 box: m:int=1:int, $i$f$foo\1\44:int=0:int
// library.kt:6 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int
// library.kt:11 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x1Var\2:int=1:int, $i$f$x1\2\141:int=0:int
// library.kt:12 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x1Var\2:int=1:int, $i$f$x1\2\141:int=0:int, y1\2:int=1:int
// library.kt:17 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x1Var\2:int=1:int, $i$f$x1\2\141:int=0:int, y1\2:int=1:int, x2Var\3:int=2:int, $i$f$x2\3\147:int=0:int
// library.kt:18 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x1Var\2:int=1:int, $i$f$x1\2\141:int=0:int, y1\2:int=1:int, x2Var\3:int=2:int, $i$f$x2\3\147:int=0:int, y2\3:int=2:int
// library.kt:13 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x1Var\2:int=1:int, $i$f$x1\2\141:int=0:int, y1\2:int=1:int
// library.kt:21 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x1Var\2:int=1:int, $i$f$x1\2\141:int=0:int, y1\2:int=1:int, x3Var\4:int=3:int, $i$f$x3\4\148:int=0:int
// library.kt:22 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x1Var\2:int=1:int, $i$f$x1\2\141:int=0:int, y1\2:int=1:int, x3Var\4:int=3:int, $i$f$x3\4\148:int=0:int, y3\4:int=3:int
// library.kt:14 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x1Var\2:int=1:int, $i$f$x1\2\141:int=0:int, y1\2:int=1:int
// library.kt:7 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int
// library.kt:25 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x4Var\5:int=4:int, $i$f$x4\5\142:int=0:int
// library.kt:26 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x4Var\5:int=4:int, $i$f$x4\5\142:int=0:int, y4\5:int=4:int
// library.kt:31 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x4Var\5:int=4:int, $i$f$x4\5\142:int=0:int, y4\5:int=4:int, x5Var\6:int=5:int, $i$f$x5\6\161:int=0:int
// library.kt:32 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x4Var\5:int=4:int, $i$f$x4\5\142:int=0:int, y4\5:int=4:int, x5Var\6:int=5:int, $i$f$x5\6\161:int=0:int, y5\6:int=5:int
// library.kt:27 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x4Var\5:int=4:int, $i$f$x4\5\142:int=0:int, y4\5:int=4:int
// library.kt:35 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x4Var\5:int=4:int, $i$f$x4\5\142:int=0:int, y4\5:int=4:int, x6Var\7:int=6:int, $i$f$x6\7\162:int=0:int
// library.kt:36 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x4Var\5:int=4:int, $i$f$x4\5\142:int=0:int, y4\5:int=4:int, x6Var\7:int=6:int, $i$f$x6\7\162:int=0:int, y6\7:int=6:int
// library.kt:28 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int, x4Var\5:int=4:int, $i$f$x4\5\142:int=0:int, y4\5:int=4:int
// library.kt:8 box: m:int=1:int, $i$f$foo\1\44:int=0:int, fooVar\1:int=100:int
// test.kt:45 box: m:int=1:int
// library.kt:5 box: m:int=1:int, $i$f$foo\8\45:int=0:int
// library.kt:6 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int
// library.kt:11 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x1Var\9:int=1:int, $i$f$x1\9\173:int=0:int
// library.kt:12 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x1Var\9:int=1:int, $i$f$x1\9\173:int=0:int, y1\9:int=1:int
// library.kt:17 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x1Var\9:int=1:int, $i$f$x1\9\173:int=0:int, y1\9:int=1:int, x2Var\10:int=2:int, $i$f$x2\10\179:int=0:int
// library.kt:18 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x1Var\9:int=1:int, $i$f$x1\9\173:int=0:int, y1\9:int=1:int, x2Var\10:int=2:int, $i$f$x2\10\179:int=0:int, y2\10:int=2:int
// library.kt:13 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x1Var\9:int=1:int, $i$f$x1\9\173:int=0:int, y1\9:int=1:int
// library.kt:21 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x1Var\9:int=1:int, $i$f$x1\9\173:int=0:int, y1\9:int=1:int, x3Var\11:int=3:int, $i$f$x3\11\180:int=0:int
// library.kt:22 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x1Var\9:int=1:int, $i$f$x1\9\173:int=0:int, y1\9:int=1:int, x3Var\11:int=3:int, $i$f$x3\11\180:int=0:int, y3\11:int=3:int
// library.kt:14 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x1Var\9:int=1:int, $i$f$x1\9\173:int=0:int, y1\9:int=1:int
// library.kt:7 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int
// library.kt:25 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x4Var\12:int=4:int, $i$f$x4\12\174:int=0:int
// library.kt:26 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x4Var\12:int=4:int, $i$f$x4\12\174:int=0:int, y4\12:int=4:int
// library.kt:31 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x4Var\12:int=4:int, $i$f$x4\12\174:int=0:int, y4\12:int=4:int, x5Var\13:int=5:int, $i$f$x5\13\193:int=0:int
// library.kt:32 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x4Var\12:int=4:int, $i$f$x4\12\174:int=0:int, y4\12:int=4:int, x5Var\13:int=5:int, $i$f$x5\13\193:int=0:int, y5\13:int=5:int
// library.kt:27 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x4Var\12:int=4:int, $i$f$x4\12\174:int=0:int, y4\12:int=4:int
// library.kt:35 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x4Var\12:int=4:int, $i$f$x4\12\174:int=0:int, y4\12:int=4:int, x6Var\14:int=6:int, $i$f$x6\14\194:int=0:int
// library.kt:36 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x4Var\12:int=4:int, $i$f$x4\12\174:int=0:int, y4\12:int=4:int, x6Var\14:int=6:int, $i$f$x6\14\194:int=0:int, y6\14:int=6:int
// library.kt:28 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int, x4Var\12:int=4:int, $i$f$x4\12\174:int=0:int, y4\12:int=4:int
// library.kt:8 box: m:int=1:int, $i$f$foo\8\45:int=0:int, fooVar\8:int=100:int
// test.kt:46 box: m:int=1:int

// EXPECTATIONS WASM
// test.kt:43 $box: $m:i32=0:i32, $fooVar:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32, $y3:i32=0:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (12, 12)
// test.kt:44 $box: $m:i32=1:i32, $fooVar:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32, $y3:i32=0:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (4)
// library.kt:5 $box: $m:i32=1:i32, $fooVar:i32=0:i32, $y1:i32=0:i32, $y2:i32=0:i32, $y3:i32=0:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (17, 17)
// library.kt:6 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=0:i32, $y2:i32=0:i32, $y3:i32=0:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (7, 7, 4, 4, 4)
// library.kt:11 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=0:i32, $y2:i32=0:i32, $y3:i32=0:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (13, 13)
// library.kt:12 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=0:i32, $y3:i32=0:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (7, 7, 4, 4, 4)
// library.kt:17 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=0:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (13, 13, 13)
// library.kt:18 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=0:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (1, 1)
// library.kt:13 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=0:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (7, 7, 4, 4, 4)
// library.kt:21 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (13, 13, 13)
// library.kt:22 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (1, 1, 1)
// library.kt:14 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (1, 1)
// library.kt:7 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (7, 7, 4, 4, 4)
// library.kt:25 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=0:i32, $y5:i32=0:i32, $y6:i32=0:i32 (13, 13)
// library.kt:26 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=0:i32, $y6:i32=0:i32 (7, 7, 4, 4, 4)
// library.kt:31 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=0:i32 (13, 13, 13)
// library.kt:32 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=0:i32 (1, 1)
// library.kt:27 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=0:i32 (7, 7, 4, 4, 4)
// library.kt:35 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (13, 13, 13)
// library.kt:36 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1, 1)
// library.kt:28 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1, 1)
// library.kt:8 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1)
// test.kt:45 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (4)
// library.kt:5 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (17, 17)
// library.kt:6 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (7, 7, 4, 4, 4)
// library.kt:11 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (13, 13)
// library.kt:12 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (7, 7, 4, 4, 4)
// library.kt:17 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (13, 13, 13)
// library.kt:18 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1)
// library.kt:13 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (7, 7, 4, 4, 4)
// library.kt:21 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (13, 13, 13)
// library.kt:22 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1, 1)
// library.kt:14 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1)
// library.kt:7 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (7, 7, 4, 4, 4)
// library.kt:25 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (13, 13)
// library.kt:26 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (7, 7, 4, 4, 4)
// library.kt:31 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (13, 13, 13)
// library.kt:32 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1)
// library.kt:27 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (7, 7, 4, 4, 4)
// library.kt:35 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (13, 13, 13)
// library.kt:36 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1, 1)
// library.kt:28 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1, 1)
// library.kt:8 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1)
// test.kt:46 $box: $m:i32=1:i32, $fooVar:i32=100:i32, $y1:i32=1:i32, $y2:i32=2:i32, $y3:i32=3:i32, $y4:i32=4:i32, $y5:i32=5:i32, $y6:i32=6:i32 (1, 1)
