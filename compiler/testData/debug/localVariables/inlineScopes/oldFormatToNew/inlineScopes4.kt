// MODULE: library
// FILE: library.kt

inline fun foo(xFoo: Int, f: (Int, Int) -> Unit) {
    val yFoo = 1
    f(xFoo, yFoo)
}

inline fun bar(xBar: Int) {
    val yBar = 2
    val dangerous = 3; foo(3) { x, y ->
        x + y
    }
}

// MODULE: test(library)
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    val m = 1
    bar(1)
    bar(2)
}

// EXPECTATIONS JVM_IR
// test.kt:21 box:
// test.kt:22 box: m:int=1:int
// library.kt:10 box: m:int=1:int, xBar\1:int=1:int, $i$f$bar\1\22:int=0:int
// library.kt:11 box: m:int=1:int, xBar\1:int=1:int, $i$f$bar\1\22:int=0:int, yBar\1:int=2:int
// library.kt:5 box: m:int=1:int, xBar\1:int=1:int, $i$f$bar\1\22:int=0:int, yBar\1:int=2:int, dangerous\1:int=3:int, xFoo\2:int=3:int, $i$f$foo\2\73:int=0:int
// library.kt:6 box: m:int=1:int, xBar\1:int=1:int, $i$f$bar\1\22:int=0:int, yBar\1:int=2:int, dangerous\1:int=3:int, xFoo\2:int=3:int, $i$f$foo\2\73:int=0:int, yFoo\2:int=1:int
// library.kt:12 box: m:int=1:int, xBar\1:int=1:int, $i$f$bar\1\22:int=0:int, yBar\1:int=2:int, dangerous\1:int=3:int, xFoo\2:int=3:int, $i$f$foo\2\73:int=0:int, yFoo\2:int=1:int, y\3:int=1:int, x\3:int=3:int, $i$a$-foo-LibraryKt$bar$1\3\75\1:int=0:int
// library.kt:13 box: m:int=1:int, xBar\1:int=1:int, $i$f$bar\1\22:int=0:int, yBar\1:int=2:int, dangerous\1:int=3:int, xFoo\2:int=3:int, $i$f$foo\2\73:int=0:int, yFoo\2:int=1:int, y\3:int=1:int, x\3:int=3:int, $i$a$-foo-LibraryKt$bar$1\3\75\1:int=0:int
// library.kt:6 box: m:int=1:int, xBar\1:int=1:int, $i$f$bar\1\22:int=0:int, yBar\1:int=2:int, dangerous\1:int=3:int, xFoo\2:int=3:int, $i$f$foo\2\73:int=0:int, yFoo\2:int=1:int
// library.kt:7 box: m:int=1:int, xBar\1:int=1:int, $i$f$bar\1\22:int=0:int, yBar\1:int=2:int, dangerous\1:int=3:int, xFoo\2:int=3:int, $i$f$foo\2\73:int=0:int, yFoo\2:int=1:int
// library.kt:14 box: m:int=1:int, xBar\1:int=1:int, $i$f$bar\1\22:int=0:int, yBar\1:int=2:int, dangerous\1:int=3:int
// test.kt:23 box: m:int=1:int
// library.kt:10 box: m:int=1:int, xBar\4:int=2:int, $i$f$bar\4\23:int=0:int
// library.kt:11 box: m:int=1:int, xBar\4:int=2:int, $i$f$bar\4\23:int=0:int, yBar\4:int=2:int
// library.kt:5 box: m:int=1:int, xBar\4:int=2:int, $i$f$bar\4\23:int=0:int, yBar\4:int=2:int, dangerous\4:int=3:int, xFoo\5:int=3:int, $i$f$foo\5\85:int=0:int
// library.kt:6 box: m:int=1:int, xBar\4:int=2:int, $i$f$bar\4\23:int=0:int, yBar\4:int=2:int, dangerous\4:int=3:int, xFoo\5:int=3:int, $i$f$foo\5\85:int=0:int, yFoo\5:int=1:int
// library.kt:12 box: m:int=1:int, xBar\4:int=2:int, $i$f$bar\4\23:int=0:int, yBar\4:int=2:int, dangerous\4:int=3:int, xFoo\5:int=3:int, $i$f$foo\5\85:int=0:int, yFoo\5:int=1:int, y\6:int=1:int, x\6:int=3:int, $i$a$-foo-LibraryKt$bar$1\6\87\4:int=0:int
// library.kt:13 box: m:int=1:int, xBar\4:int=2:int, $i$f$bar\4\23:int=0:int, yBar\4:int=2:int, dangerous\4:int=3:int, xFoo\5:int=3:int, $i$f$foo\5\85:int=0:int, yFoo\5:int=1:int, y\6:int=1:int, x\6:int=3:int, $i$a$-foo-LibraryKt$bar$1\6\87\4:int=0:int
// library.kt:6 box: m:int=1:int, xBar\4:int=2:int, $i$f$bar\4\23:int=0:int, yBar\4:int=2:int, dangerous\4:int=3:int, xFoo\5:int=3:int, $i$f$foo\5\85:int=0:int, yFoo\5:int=1:int
// library.kt:7 box: m:int=1:int, xBar\4:int=2:int, $i$f$bar\4\23:int=0:int, yBar\4:int=2:int, dangerous\4:int=3:int, xFoo\5:int=3:int, $i$f$foo\5\85:int=0:int, yFoo\5:int=1:int
// library.kt:14 box: m:int=1:int, xBar\4:int=2:int, $i$f$bar\4\23:int=0:int, yBar\4:int=2:int, dangerous\4:int=3:int
// test.kt:24 box: m:int=1:int

// EXPECTATIONS WASM
// test.kt:21 $box: $m:i32=0:i32, $yBar:i32=0:i32, $dangerous:i32=0:i32, $yFoo:i32=0:i32 (12, 12)
// test.kt:22 $box: $m:i32=1:i32, $yBar:i32=0:i32, $dangerous:i32=0:i32, $yFoo:i32=0:i32 (8, 8, 4, 4, 4)
// library.kt:10 $box: $m:i32=1:i32, $yBar:i32=0:i32, $dangerous:i32=0:i32, $yFoo:i32=0:i32 (15, 15)
// library.kt:11 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=0:i32 (20, 20, 27, 27, 23, 23, 23)
// library.kt:5 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=0:i32 (15, 15)
// library.kt:6 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (6, 6, 12, 12, 4, 4, 4, 4, 4)
// library.kt:12 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (8, 12, 8, 8, 8)
// library.kt:13 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (5, 5, 5)
// library.kt:7 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (1, 1, 1)
// library.kt:14 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (1, 1)
// test.kt:23 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (8, 8, 4, 4, 4)
// library.kt:10 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (15, 15)
// library.kt:11 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (20, 20, 27, 27, 23, 23, 23)
// library.kt:5 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (15, 15)
// library.kt:6 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (6, 6, 12, 12, 4, 4, 4, 4, 4)
// library.kt:12 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (8, 12, 8, 8, 8)
// library.kt:13 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (5, 5, 5)
// library.kt:7 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (1, 1, 1)
// library.kt:14 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (1, 1)
// test.kt:24 $box: $m:i32=1:i32, $yBar:i32=2:i32, $dangerous:i32=3:i32, $yFoo:i32=1:i32 (1, 1)
