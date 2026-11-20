// TARGET_BACKEND: JVM
// USE_INLINE_SCOPES_NUMBERS
// WITH_STDLIB
// FILE: test.kt
// KT-86377: the receiver `$this$...` of each `with` lambda must carry the scope number of its own
// lambda marker. It starts before that marker, but it must not go to the enclosing scope.

inline fun f() {
    val x = 1
    with("A") {
        val a = 2
        with("B") {
            val b = 3
            g()
        }
    }
}

inline fun g() {
    val y = 4
}

fun box() {
    f()
}

// EXPECTATIONS JVM
// test.kt:24 box:
// test.kt:9 box: $i$f$f\1\24:int=0:int
// test.kt:10 box: $i$f$f\1\24:int=0:int, x\1:int=1:int
// test.kt:11 box: $i$f$f\1\24:int=0:int, x\1:int=1:int, $this$f_u24lambda_u240\2:java.lang.String="A":java.lang.String, $i$a$-with-TestKt$f$1\2\47\1:int=0:int
// test.kt:12 box: $i$f$f\1\24:int=0:int, x\1:int=1:int, $this$f_u24lambda_u240\2:java.lang.String="A":java.lang.String, $i$a$-with-TestKt$f$1\2\47\1:int=0:int, a\2:int=2:int
// test.kt:13 box: $i$f$f\1\24:int=0:int, x\1:int=1:int, $this$f_u24lambda_u240\2:java.lang.String="A":java.lang.String, $i$a$-with-TestKt$f$1\2\47\1:int=0:int, a\2:int=2:int, $this$f_u24lambda_u240_u240\3:java.lang.String="B":java.lang.String, $i$a$-with-TestKt$f$1$1\3\49\2:int=0:int
// test.kt:14 box: $i$f$f\1\24:int=0:int, x\1:int=1:int, $this$f_u24lambda_u240\2:java.lang.String="A":java.lang.String, $i$a$-with-TestKt$f$1\2\47\1:int=0:int, a\2:int=2:int, $this$f_u24lambda_u240_u240\3:java.lang.String="B":java.lang.String, $i$a$-with-TestKt$f$1$1\3\49\2:int=0:int, b\3:int=3:int
// test.kt:20 box: $i$f$f\1\24:int=0:int, x\1:int=1:int, $this$f_u24lambda_u240\2:java.lang.String="A":java.lang.String, $i$a$-with-TestKt$f$1\2\47\1:int=0:int, a\2:int=2:int, $this$f_u24lambda_u240_u240\3:java.lang.String="B":java.lang.String, $i$a$-with-TestKt$f$1$1\3\49\2:int=0:int, b\3:int=3:int, $i$f$g\4\51:int=0:int
// test.kt:21 box: $i$f$f\1\24:int=0:int, x\1:int=1:int, $this$f_u24lambda_u240\2:java.lang.String="A":java.lang.String, $i$a$-with-TestKt$f$1\2\47\1:int=0:int, a\2:int=2:int, $this$f_u24lambda_u240_u240\3:java.lang.String="B":java.lang.String, $i$a$-with-TestKt$f$1$1\3\49\2:int=0:int, b\3:int=3:int, $i$f$g\4\51:int=0:int, y\4:int=4:int
// test.kt:15 box: $i$f$f\1\24:int=0:int, x\1:int=1:int, $this$f_u24lambda_u240\2:java.lang.String="A":java.lang.String, $i$a$-with-TestKt$f$1\2\47\1:int=0:int, a\2:int=2:int, $this$f_u24lambda_u240_u240\3:java.lang.String="B":java.lang.String, $i$a$-with-TestKt$f$1$1\3\49\2:int=0:int, b\3:int=3:int
// test.kt:12 box: $i$f$f\1\24:int=0:int, x\1:int=1:int, $this$f_u24lambda_u240\2:java.lang.String="A":java.lang.String, $i$a$-with-TestKt$f$1\2\47\1:int=0:int, a\2:int=2:int
// test.kt:16 box: $i$f$f\1\24:int=0:int, x\1:int=1:int, $this$f_u24lambda_u240\2:java.lang.String="A":java.lang.String, $i$a$-with-TestKt$f$1\2\47\1:int=0:int, a\2:int=2:int
// test.kt:10 box: $i$f$f\1\24:int=0:int, x\1:int=1:int
// test.kt:17 box: $i$f$f\1\24:int=0:int, x\1:int=1:int
// test.kt:25 box:
