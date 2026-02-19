// MODULE: library
// USE_INLINE_SCOPES_NUMBERS
// FILE: library.kt

inline fun foo(xFoo: Int, f: (Int) -> Unit, g: (Int) -> Unit) {
    bar(0, 1, 2)
    f(1)
    bar(1, 2, 3)
    g(2)
    bar(2, 3, 4)
}

inline fun bar(xBar1: Int, xBar2: Int, xBar3: Int) {
    baz(100, 101, 102)
}

inline fun baz(xBaz1: Int, xBaz2: Int, xBaz3: Int) {
    x1()
    x2()
}

inline fun x1() {
    val x1 = 1
}

inline fun x2() {
    val x2 = 2
}

// MODULE: test(library)
// FILE: test.kt

fun box() {
    foo(1, {
        val y1 = 1
        bar(0, 1, 2)
    }, {
        val y2 = 2
        bar(1, 2, 3)
    })

    foo(1, {
        val y1 = 1
        bar(0, 1, 2)
    }, {
        val y2 = 2
        bar(1, 2, 3)
    })
}

// EXPECTATIONS JVM_IR
// test.kt:34 box:
// library.kt:6 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int, $i$f$x1\3\35:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int, $i$f$x1\3\35:int=0:int, x1\3$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int, $i$f$x2\4\36:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int, $i$f$x2\4\36:int=0:int, x2\4$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int
// library.kt:7 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// test.kt:35 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int
// test.kt:36 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x1\2\76:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x1\2\76:int=0:int, x1\2$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x2\3\77:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x2\3\77:int=0:int, x2\3$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int
// test.kt:37 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, y1:int=1:int
// library.kt:7 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:8 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int, $i$f$x1\7\50:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int, $i$f$x1\7\50:int=0:int, x1\7$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int, $i$f$x2\8\51:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int, $i$f$x2\8\51:int=0:int, x2\8$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int
// library.kt:9 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// test.kt:38 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int
// test.kt:39 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x1\2\76:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x1\2\76:int=0:int, x1\2$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x2\3\77:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x2\3\77:int=0:int, x2\3$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int
// test.kt:40 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$2:int=0:int, y2:int=2:int
// library.kt:9 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:10 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int, $i$f$x1\11\65:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int, $i$f$x1\11\65:int=0:int, x1\11$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int, $i$f$x2\12\66:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int, $i$f$x2\12\66:int=0:int, x2\12$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int
// library.kt:11 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// test.kt:42 box:
// library.kt:6 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int, $i$f$x1\3\35:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int, $i$f$x1\3\35:int=0:int, x1\3$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int, $i$f$x2\4\36:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int, $i$f$x2\4\36:int=0:int, x2\4$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int, xBaz1\2$iv:int=100:int, xBaz2\2$iv:int=101:int, xBaz3\2$iv:int=102:int, $i$f$baz\2\31:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\1$iv:int=0:int, xBar2\1$iv:int=1:int, xBar3\1$iv:int=2:int, $i$f$bar\1\6:int=0:int
// library.kt:7 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// test.kt:43 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int
// test.kt:44 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x1\2\76:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x1\2\76:int=0:int, x1\2$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x2\3\77:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x2\3\77:int=0:int, x2\3$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int, xBar1$iv:int=0:int, xBar2$iv:int=1:int, xBar3$iv:int=2:int, $i$f$bar:int=0:int
// test.kt:45 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=1:int, $i$a$-foo-TestKt$box$3:int=0:int, y1:int=1:int
// library.kt:7 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:8 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int, $i$f$x1\7\50:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int, $i$f$x1\7\50:int=0:int, x1\7$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int, $i$f$x2\8\51:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int, $i$f$x2\8\51:int=0:int, x2\8$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int, xBaz1\6$iv:int=100:int, xBaz2\6$iv:int=101:int, xBaz3\6$iv:int=102:int, $i$f$baz\6\46:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\5$iv:int=1:int, xBar2\5$iv:int=2:int, xBar3\5$iv:int=3:int, $i$f$bar\5\8:int=0:int
// library.kt:9 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// test.kt:46 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int
// test.kt:47 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x1\2\76:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x1\2\76:int=0:int, x1\2$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x2\3\77:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int, $i$f$x2\3\77:int=0:int, x2\3$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int, xBaz1\1$iv:int=100:int, xBaz2\1$iv:int=101:int, xBaz3\1$iv:int=102:int, $i$f$baz\1\14:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int, xBar1$iv:int=1:int, xBar2$iv:int=2:int, xBar3$iv:int=3:int, $i$f$bar:int=0:int
// test.kt:48 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, it:int=2:int, $i$a$-foo-TestKt$box$4:int=0:int, y2:int=2:int
// library.kt:9 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:10 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:14 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int
// library.kt:18 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int
// library.kt:23 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int, $i$f$x1\11\65:int=0:int
// library.kt:24 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int, $i$f$x1\11\65:int=0:int, x1\11$iv:int=1:int
// library.kt:19 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int
// library.kt:27 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int, $i$f$x2\12\66:int=0:int
// library.kt:28 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int, $i$f$x2\12\66:int=0:int, x2\12$iv:int=2:int
// library.kt:20 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int, xBaz1\10$iv:int=100:int, xBaz2\10$iv:int=101:int, xBaz3\10$iv:int=102:int, $i$f$baz\10\61:int=0:int
// library.kt:15 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int, xBar1\9$iv:int=2:int, xBar2\9$iv:int=3:int, xBar3\9$iv:int=4:int, $i$f$bar\9\10:int=0:int
// library.kt:11 box: xFoo$iv:int=1:int, $i$f$foo:int=0:int
// test.kt:49 box:
