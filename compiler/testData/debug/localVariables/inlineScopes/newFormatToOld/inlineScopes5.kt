// MODULE: library
// USE_INLINE_SCOPES_NUMBERS
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
// FILE: test.kt

fun box() {
    val m = 1
    foo()
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:43 box:
// test.kt:44 box: m:int=1:int
// library.kt:6 box: m:int=1:int, $i$f$foo:int=0:int
// library.kt:7 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int
// library.kt:12 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int
// library.kt:13 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int
// library.kt:18 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int, x2Var\2$iv:int=2:int, $i$f$x2\2\41:int=0:int
// library.kt:19 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int, x2Var\2$iv:int=2:int, $i$f$x2\2\41:int=0:int, y2\2$iv:int=2:int
// library.kt:14 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int
// library.kt:22 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int, x3Var\3$iv:int=3:int, $i$f$x3\3\42:int=0:int
// library.kt:23 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int, x3Var\3$iv:int=3:int, $i$f$x3\3\42:int=0:int, y3\3$iv:int=3:int
// library.kt:15 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int
// library.kt:8 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int
// library.kt:26 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int
// library.kt:27 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int
// library.kt:32 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int, x5Var\5$iv:int=5:int, $i$f$x5\5\53:int=0:int
// library.kt:33 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int, x5Var\5$iv:int=5:int, $i$f$x5\5\53:int=0:int, y5\5$iv:int=5:int
// library.kt:28 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int
// library.kt:36 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int, x6Var\6$iv:int=6:int, $i$f$x6\6\54:int=0:int
// library.kt:37 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int, x6Var\6$iv:int=6:int, $i$f$x6\6\54:int=0:int, y6\6$iv:int=6:int
// library.kt:29 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int
// library.kt:9 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int
// test.kt:45 box: m:int=1:int
// library.kt:6 box: m:int=1:int, $i$f$foo:int=0:int
// library.kt:7 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int
// library.kt:12 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int
// library.kt:13 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int
// library.kt:18 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int, x2Var\2$iv:int=2:int, $i$f$x2\2\41:int=0:int
// library.kt:19 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int, x2Var\2$iv:int=2:int, $i$f$x2\2\41:int=0:int, y2\2$iv:int=2:int
// library.kt:14 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int
// library.kt:22 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int, x3Var\3$iv:int=3:int, $i$f$x3\3\42:int=0:int
// library.kt:23 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int, x3Var\3$iv:int=3:int, $i$f$x3\3\42:int=0:int, y3\3$iv:int=3:int
// library.kt:15 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x1Var\1$iv:int=1:int, $i$f$x1\1\7:int=0:int, y1\1$iv:int=1:int
// library.kt:8 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int
// library.kt:26 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int
// library.kt:27 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int
// library.kt:32 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int, x5Var\5$iv:int=5:int, $i$f$x5\5\53:int=0:int
// library.kt:33 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int, x5Var\5$iv:int=5:int, $i$f$x5\5\53:int=0:int, y5\5$iv:int=5:int
// library.kt:28 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int
// library.kt:36 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int, x6Var\6$iv:int=6:int, $i$f$x6\6\54:int=0:int
// library.kt:37 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int, x6Var\6$iv:int=6:int, $i$f$x6\6\54:int=0:int, y6\6$iv:int=6:int
// library.kt:29 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int, x4Var\4$iv:int=4:int, $i$f$x4\4\8:int=0:int, y4\4$iv:int=4:int
// library.kt:9 box: m:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=100:int
// test.kt:46 box: m:int=1:int
