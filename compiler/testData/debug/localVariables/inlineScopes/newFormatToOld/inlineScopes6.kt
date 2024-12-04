// MODULE: library
// USE_INLINE_SCOPES_NUMBERS
// FILE: library.kt

inline fun foo(f: (Int) -> Unit) {
    val x6 = 6
    f(8)
    val x7 = 7
}

inline fun bar() {
    val x0 = 0
    x1()
}

inline fun baz() {
    x4()
    x5()
}

inline fun x1() {
    val x1 = 1
    x2()
    val x3 = 3
}

inline fun x2() {
    val x2 = 2
}

inline fun x4() {
    val x4 = 4
}

inline fun x5() {
    val x5 = 5
}

// MODULE: test(library)
// FILE: test.kt

fun box() {
    val m = -1
    bar()
    foo {
        val x8 = 8
    }
    baz()
}

// EXPECTATIONS JVM_IR
// test.kt:43 box:
// test.kt:44 box: m:int=-1:int
// library.kt:12 box: m:int=-1:int, $i$f$bar:int=0:int
// library.kt:13 box: m:int=-1:int, $i$f$bar:int=0:int, x0$iv:int=0:int
// library.kt:22 box: m:int=-1:int, $i$f$bar:int=0:int, x0$iv:int=0:int, $i$f$x1\1\13:int=0:int
// library.kt:23 box: m:int=-1:int, $i$f$bar:int=0:int, x0$iv:int=0:int, $i$f$x1\1\13:int=0:int, x1\1$iv:int=1:int
// library.kt:28 box: m:int=-1:int, $i$f$bar:int=0:int, x0$iv:int=0:int, $i$f$x1\1\13:int=0:int, x1\1$iv:int=1:int, $i$f$x2\2\41:int=0:int
// library.kt:29 box: m:int=-1:int, $i$f$bar:int=0:int, x0$iv:int=0:int, $i$f$x1\1\13:int=0:int, x1\1$iv:int=1:int, $i$f$x2\2\41:int=0:int, x2\2$iv:int=2:int
// library.kt:24 box: m:int=-1:int, $i$f$bar:int=0:int, x0$iv:int=0:int, $i$f$x1\1\13:int=0:int, x1\1$iv:int=1:int
// library.kt:25 box: m:int=-1:int, $i$f$bar:int=0:int, x0$iv:int=0:int, $i$f$x1\1\13:int=0:int, x1\1$iv:int=1:int, x3\1$iv:int=3:int
// library.kt:14 box: m:int=-1:int, $i$f$bar:int=0:int, x0$iv:int=0:int
// test.kt:45 box: m:int=-1:int
// library.kt:6 box: m:int=-1:int, $i$f$foo:int=0:int
// library.kt:7 box: m:int=-1:int, $i$f$foo:int=0:int, x6$iv:int=6:int
// test.kt:46 box: m:int=-1:int, $i$f$foo:int=0:int, x6$iv:int=6:int, it:int=8:int, $i$a$-foo-TestKt$box$1:int=0:int
// test.kt:47 box: m:int=-1:int, $i$f$foo:int=0:int, x6$iv:int=6:int, it:int=8:int, $i$a$-foo-TestKt$box$1:int=0:int, x8:int=8:int
// library.kt:7 box: m:int=-1:int, $i$f$foo:int=0:int, x6$iv:int=6:int
// library.kt:8 box: m:int=-1:int, $i$f$foo:int=0:int, x6$iv:int=6:int
// library.kt:9 box: m:int=-1:int, $i$f$foo:int=0:int, x6$iv:int=6:int, x7$iv:int=7:int
// test.kt:48 box: m:int=-1:int
// library.kt:17 box: m:int=-1:int, $i$f$baz:int=0:int
// library.kt:32 box: m:int=-1:int, $i$f$baz:int=0:int, $i$f$x4\1\17:int=0:int
// library.kt:33 box: m:int=-1:int, $i$f$baz:int=0:int, $i$f$x4\1\17:int=0:int, x4\1$iv:int=4:int
// library.kt:18 box: m:int=-1:int, $i$f$baz:int=0:int
// library.kt:36 box: m:int=-1:int, $i$f$baz:int=0:int, $i$f$x5\2\18:int=0:int
// library.kt:37 box: m:int=-1:int, $i$f$baz:int=0:int, $i$f$x5\2\18:int=0:int, x5\2$iv:int=5:int
// library.kt:19 box: m:int=-1:int, $i$f$baz:int=0:int
// test.kt:49 box: m:int=-1:int
