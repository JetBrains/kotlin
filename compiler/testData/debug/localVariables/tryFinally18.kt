// IGNORE_INLINER: IR
// TARGET_PLATFORM: JVM

// WITH_STDLIB
// FILE: test.kt

inline fun <R> analyze(action: () -> R): R {
    try {
        return action()
    } finally {
        println("End")
    }
}


private fun callSite(a: Int, b: Int) {
    analyze {
        foo(a) ?: return // <-- non-local return, copy of finally-block is inlined here
        println("$a $b")
        42
    }
}

fun foo(a: Int): Int? = if (a == 1) 42 else null

fun box() {
    callSite(1, 2)
    callSite(2, 2)
}

// EXPECTATIONS JVM_IR
// test.kt:27 box:
// test.kt:17 callSite: a:int=1:int, b:int=2:int
// test.kt:8 callSite: a:int=1:int, b:int=2:int, $i$f$analyze:int=0:int
// test.kt:9 callSite: a:int=1:int, b:int=2:int, $i$f$analyze:int=0:int
// test.kt:18 callSite: a:int=1:int, b:int=2:int, $i$f$analyze:int=0:int, $i$a$-analyze-TestKt$callSite$1:int=0:int
// test.kt:24 foo: a:int=1:int
// test.kt:18 callSite: a:int=1:int, b:int=2:int, $i$f$analyze:int=0:int, $i$a$-analyze-TestKt$callSite$1:int=0:int
// test.kt:19 callSite: a:int=1:int, b:int=2:int, $i$f$analyze:int=0:int, $i$a$-analyze-TestKt$callSite$1:int=0:int
// test.kt:20 callSite: a:int=1:int, b:int=2:int, $i$f$analyze:int=0:int, $i$a$-analyze-TestKt$callSite$1:int=0:int
// test.kt:9 callSite: a:int=1:int, b:int=2:int, $i$f$analyze:int=0:int
// test.kt:11 callSite: a:int=1:int, b:int=2:int, $i$f$analyze:int=0:int
// test.kt:9 callSite: a:int=1:int, b:int=2:int, $i$f$analyze:int=0:int
// test.kt:22 callSite: a:int=1:int, b:int=2:int
// test.kt:28 box:
// test.kt:17 callSite: a:int=2:int, b:int=2:int
// test.kt:8 callSite: a:int=2:int, b:int=2:int, $i$f$analyze:int=0:int
// test.kt:9 callSite: a:int=2:int, b:int=2:int, $i$f$analyze:int=0:int
// test.kt:18 callSite: a:int=2:int, b:int=2:int, $i$f$analyze:int=0:int, $i$a$-analyze-TestKt$callSite$1:int=0:int
// test.kt:24 foo: a:int=2:int
// test.kt:18 callSite: a:int=2:int, b:int=2:int, $i$f$analyze:int=0:int, $i$a$-analyze-TestKt$callSite$1:int=0:int
// test.kt:11 callSite: a:int=2:int, b:int=2:int, $i$f$analyze:int=0:int
// test.kt:29 box:

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:27 box:

// test.kt:17 callSite: a:int=1:int, b:int=2:int
// test.kt:8 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:9 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:18 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\102\0:int=0:int
// test.kt:24 foo: a:int=1:int
// test.kt:18 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\102\0:int=0:int
// test.kt:19 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\102\0:int=0:int
// test.kt:20 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\102\0:int=0:int
// test.kt:9 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:11 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:9 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:22 callSite: a:int=1:int, b:int=2:int
// test.kt:28 box:
// test.kt:17 callSite: a:int=2:int, b:int=2:int
// test.kt:8 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:9 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:18 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\102\0:int=0:int
// test.kt:24 foo: a:int=2:int
// test.kt:18 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\102\0:int=0:int
// test.kt:11 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:29 box:

// EXPECTATIONS WASM
// test.kt:27 $box: (13, 16, 4)
// test.kt:17 $callSite: $a:i32=1:i32, $b:i32=2:i32 (4, 4, 4, 4, 4)
// test.kt:9 $callSite: $a:i32=1:i32, $b:i32=2:i32 (15)
// test.kt:18 $callSite: $a:i32=1:i32, $b:i32=2:i32 (12, 8)
// test.kt:24 $foo: $a:i32=1:i32 (28, 33, 28, 28, 36, 36, 36, 36, 36, 36, 48)
// test.kt:18 $callSite: $a:i32=1:i32, $b:i32=2:i32 (8, 8, 8, 8, 8)
// test.kt:19 $callSite: $a:i32=1:i32, $b:i32=2:i32 (18, 18, 18, 18, 18, 18, 18, 16, 16, 16, 16, 16, 19, 19, 19, 16, 21, 21, 21, 21, 21, 21, 16, 8, 8, 8)
// test.kt:20 $callSite: $a:i32=1:i32, $b:i32=2:i32 (8, 8, 8, 8, 8, 8, 10)
// test.kt:9 $callSite: $a:i32=1:i32, $b:i32=2:i32 (15, 15, 8)
// test.kt:11 $callSite: $a:i32=1:i32, $b:i32=2:i32 (8, 16, 16, 16, 8, 8, 8, 8)
// test.kt:22 $callSite: $a:i32=1:i32, $b:i32=2:i32 (1, 1)
// test.kt:28 $box: (13, 16, 4)
// test.kt:17 $callSite: $a:i32=2:i32, $b:i32=2:i32 (4, 4, 4, 4, 4)
// test.kt:9 $callSite: $a:i32=2:i32, $b:i32=2:i32 (15)
// test.kt:18 $callSite: $a:i32=2:i32, $b:i32=2:i32 (12, 8)
// test.kt:24 $foo: $a:i32=2:i32 (28, 33, 28, 28, 44, 48, 48)
// test.kt:18 $callSite: $a:i32=2:i32, $b:i32=2:i32 (8, 8, 8, 8, 18)
// test.kt:11 $callSite: $a:i32=2:i32, $b:i32=2:i32 (8, 16, 16, 16, 8, 8)
// test.kt:29 $box: (1)
