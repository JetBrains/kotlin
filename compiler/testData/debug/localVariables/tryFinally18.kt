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
// test.kt:18 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\81\0:int=0:int
// test.kt:24 foo: a:int=1:int
// test.kt:18 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\81\0:int=0:int
// test.kt:19 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\81\0:int=0:int
// test.kt:20 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\81\0:int=0:int
// test.kt:9 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:11 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:9 callSite: a:int=1:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:22 callSite: a:int=1:int, b:int=2:int
// test.kt:28 box:
// test.kt:17 callSite: a:int=2:int, b:int=2:int
// test.kt:8 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:9 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:18 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\81\0:int=0:int
// test.kt:24 foo: a:int=2:int
// test.kt:18 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int, $i$a$-analyze-TestKt$callSite$1\2\81\0:int=0:int
// test.kt:11 callSite: a:int=2:int, b:int=2:int, $i$f$analyze\1\17:int=0:int
// test.kt:29 box:
