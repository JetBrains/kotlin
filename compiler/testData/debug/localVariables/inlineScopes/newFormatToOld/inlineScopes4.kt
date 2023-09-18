// MODULE: library
// USE_INLINE_SCOPES_NUMBERS
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
// FILE: test.kt

fun box() {
    val m = 1
    bar(1)
    bar(2)
}

// EXPECTATIONS JVM_IR
// test.kt:21 box:
// test.kt:22 box: m:int=1:int
// library.kt:11 box: m:int=1:int, xBar$iv:int=1:int, $i$f$bar:int=0:int
// library.kt:12 box: m:int=1:int, xBar$iv:int=1:int, $i$f$bar:int=0:int, yBar$iv:int=2:int
// library.kt:6 box: m:int=1:int, xBar$iv:int=1:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int
// library.kt:7 box: m:int=1:int, xBar$iv:int=1:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int
// library.kt:13 box: m:int=1:int, xBar$iv:int=1:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int, y\2$iv:int=1:int, x\2$iv:int=3:int, $i$a$-foo-LibraryKt$bar$1\2\19\0$iv:int=0:int
// library.kt:14 box: m:int=1:int, xBar$iv:int=1:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int, y\2$iv:int=1:int, x\2$iv:int=3:int, $i$a$-foo-LibraryKt$bar$1\2\19\0$iv:int=0:int
// library.kt:7 box: m:int=1:int, xBar$iv:int=1:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int
// library.kt:8 box: m:int=1:int, xBar$iv:int=1:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int
// library.kt:15 box: m:int=1:int, xBar$iv:int=1:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int
// test.kt:23 box: m:int=1:int
// library.kt:11 box: m:int=1:int, xBar$iv:int=2:int, $i$f$bar:int=0:int
// library.kt:12 box: m:int=1:int, xBar$iv:int=2:int, $i$f$bar:int=0:int, yBar$iv:int=2:int
// library.kt:6 box: m:int=1:int, xBar$iv:int=2:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int
// library.kt:7 box: m:int=1:int, xBar$iv:int=2:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int
// library.kt:13 box: m:int=1:int, xBar$iv:int=2:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int, y\2$iv:int=1:int, x\2$iv:int=3:int, $i$a$-foo-LibraryKt$bar$1\2\19\0$iv:int=0:int
// library.kt:14 box: m:int=1:int, xBar$iv:int=2:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int, y\2$iv:int=1:int, x\2$iv:int=3:int, $i$a$-foo-LibraryKt$bar$1\2\19\0$iv:int=0:int
// library.kt:7 box: m:int=1:int, xBar$iv:int=2:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int
// library.kt:8 box: m:int=1:int, xBar$iv:int=2:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int, xFoo\1$iv:int=3:int, $i$f$foo\1\12:int=0:int, yFoo\1$iv:int=1:int
// library.kt:15 box: m:int=1:int, xBar$iv:int=2:int, $i$f$bar:int=0:int, yBar$iv:int=2:int, dangerous$iv:int=3:int
// test.kt:24 box: m:int=1:int
