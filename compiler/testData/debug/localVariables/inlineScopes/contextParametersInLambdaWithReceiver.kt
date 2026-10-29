// TARGET_BACKEND: JVM
// USE_INLINE_SCOPES_NUMBERS
// WITH_STDLIB
// FILE: test.kt
// KT-86378: the receivers `$this$...` of the `with` lambdas must carry the scope numbers of their own
// lambda markers. The context parameters of `foo` must carry the scope number of `foo`.

class Ctx1
class Ctx2

context(ctx1: Ctx1, ctx2: Ctx2)
inline fun foo() {
    val x = 1
}

fun box() {
    with(Ctx1()) {
        with(Ctx2()) {
            foo()
        }
    }
}

// EXPECTATIONS JVM
// test.kt:17 box:
// test.kt:8 <init>:
// test.kt:17 box:
// test.kt:18 box: $this$box_u24lambda_u240\1:Ctx1=Ctx1, $i$a$-with-TestKt$box$1\1\17\0:int=0:int
// test.kt:9 <init>:
// test.kt:18 box: $this$box_u24lambda_u240\1:Ctx1=Ctx1, $i$a$-with-TestKt$box$1\1\17\0:int=0:int
// test.kt:19 box: $this$box_u24lambda_u240\1:Ctx1=Ctx1, $i$a$-with-TestKt$box$1\1\17\0:int=0:int, $this$box_u24lambda_u240_u240\2:Ctx2=Ctx2, $i$a$-with-TestKt$box$1$1\2\18\1:int=0:int
// test.kt:13 box: $this$box_u24lambda_u240\1:Ctx1=Ctx1, $i$a$-with-TestKt$box$1\1\17\0:int=0:int, $this$box_u24lambda_u240_u240\2:Ctx2=Ctx2, $i$a$-with-TestKt$box$1$1\2\18\1:int=0:int, ctx1\3:Ctx1=Ctx1, ctx2\3:Ctx2=Ctx2, $i$f$foo\3\19:int=0:int
// test.kt:14 box: $this$box_u24lambda_u240\1:Ctx1=Ctx1, $i$a$-with-TestKt$box$1\1\17\0:int=0:int, $this$box_u24lambda_u240_u240\2:Ctx2=Ctx2, $i$a$-with-TestKt$box$1$1\2\18\1:int=0:int, ctx1\3:Ctx1=Ctx1, ctx2\3:Ctx2=Ctx2, $i$f$foo\3\19:int=0:int, x\3:int=1:int
// test.kt:20 box: $this$box_u24lambda_u240\1:Ctx1=Ctx1, $i$a$-with-TestKt$box$1\1\17\0:int=0:int, $this$box_u24lambda_u240_u240\2:Ctx2=Ctx2, $i$a$-with-TestKt$box$1$1\2\18\1:int=0:int
// test.kt:18 box: $this$box_u24lambda_u240\1:Ctx1=Ctx1, $i$a$-with-TestKt$box$1\1\17\0:int=0:int
// test.kt:21 box: $this$box_u24lambda_u240\1:Ctx1=Ctx1, $i$a$-with-TestKt$box$1\1\17\0:int=0:int
// test.kt:17 box:
// test.kt:22 box:
