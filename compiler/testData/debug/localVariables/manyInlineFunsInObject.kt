// FILE: test.kt
inline fun bar(crossinline block: () -> Unit) {
    object {
        fun baz(param: Int) {
            val b = 2
            block()
            inlineCall {
                val g = 7
            }
            block()
            inlineCall {
                val g = 7
            }
            block()
            inlineCall {
                val g = 7
            }
        }
    }.baz(6)
}

inline fun inlineCall(block: () -> Unit) {
    val e = 5
    block()
}

fun box() {
    bar() {
        val d = 4
    }
}

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:28 box:
// test.kt:3 box: $i$f$bar\1\28:int=0:int
// test.kt:3 <init>:
// test.kt:19 box: $i$f$bar\1\28:int=0:int
// test.kt:5 baz: param:int=6:int
// test.kt:6 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\1\6\0:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\1\6\0:int=0:int, d\1:int=4:int
// test.kt:6 baz: param:int=6:int, b:int=2:int
// test.kt:7 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int, e\2:int=5:int
// test.kt:8 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int, e\2:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1\3\182\0:int=0:int
// test.kt:9 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int, e\2:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1\3\182\0:int=0:int, g\3:int=7:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int, e\2:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int, e\2:int=5:int
// test.kt:10 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\4\10\0:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\4\10\0:int=0:int, d\4:int=4:int
// test.kt:10 baz: param:int=6:int, b:int=2:int
// test.kt:11 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int
// test.kt:12 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$2\6\185\0:int=0:int
// test.kt:13 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$2\6\185\0:int=0:int, g\6:int=7:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\7\14\0:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\7\14\0:int=0:int, d\7:int=4:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:15 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int, e\8:int=5:int
// test.kt:16 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int, e\8:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$3\9\188\0:int=0:int
// test.kt:17 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int, e\8:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$3\9\188\0:int=0:int, g\9:int=7:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int, e\8:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int, e\8:int=5:int
// test.kt:18 baz: param:int=6:int, b:int=2:int
// test.kt:20 box: $i$f$bar\1\28:int=0:int
// test.kt:31 box:

// EXPECTATIONS JVM_IR
// test.kt:28 box:
// test.kt:3 box: $i$f$bar:int=0:int
// test.kt:3 <init>:
// test.kt:19 box: $i$f$bar:int=0:int
// test.kt:5 baz: param:int=6:int
// test.kt:6 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int, d:int=4:int
// test.kt:6 baz: param:int=6:int, b:int=2:int
// test.kt:7 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:8 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1:int=0:int
// test.kt:9 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1:int=0:int, g:int=7:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:10 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int, d:int=4:int
// test.kt:10 baz: param:int=6:int, b:int=2:int
// test.kt:11 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:12 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$2:int=0:int
// test.kt:13 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$2:int=0:int, g:int=7:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int, d:int=4:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:15 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:16 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$3:int=0:int
// test.kt:17 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$3:int=0:int, g:int=7:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:18 baz: param:int=6:int, b:int=2:int
// test.kt:20 box: $i$f$bar:int=0:int
// test.kt:31 box:

// EXPECTATIONS JVM
// test.kt:28 box:
// test.kt:3 box: $i$f$bar:int=0:int
// test.kt:3 <init>:
// test.kt:19 box: $i$f$bar:int=0:int
// test.kt:5 baz: param:int=6:int
// test.kt:6 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:6 baz: param:int=6:int, b:int=2:int
// test.kt:7 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:8 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1:int=0:int
// test.kt:9 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:10 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:10 baz: param:int=6:int, b:int=2:int
// test.kt:11 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:12 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$2:int=0:int
// test.kt:13 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$2:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:15 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:16 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$3:int=0:int
// test.kt:17 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$3:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:18 baz: param:int=6:int, b:int=2:int
// test.kt:20 box: $i$f$bar:int=0:int
// test.kt:31 box:

// EXPECTATIONS JS_IR
// test.kt:19 box:
// test.kt:3 <init>:
// test.kt:19 box:
// test.kt:5 baz: param=6:number
// test.kt:29 baz: param=6:number, b=2:number
// test.kt:23 baz: param=6:number, b=2:number, d=4:number
// test.kt:8 baz: param=6:number, b=2:number, d=4:number, e=5:number
// test.kt:29 baz: param=6:number, b=2:number, d=4:number, e=5:number, g=7:number
// test.kt:23 baz: param=6:number, b=2:number, d=4:number, e=5:number, g=7:number, d=4:number
// test.kt:12 baz: param=6:number, b=2:number, d=4:number, e=5:number, g=7:number, d=4:number, e=5:number
// test.kt:29 baz: param=6:number, b=2:number, d=4:number, e=5:number, g=7:number, d=4:number, e=5:number, g=7:number
// test.kt:23 baz: param=6:number, b=2:number, d=4:number, e=5:number, g=7:number, d=4:number, e=5:number, g=7:number, d=4:number
// test.kt:16 baz: param=6:number, b=2:number, d=4:number, e=5:number, g=7:number, d=4:number, e=5:number, g=7:number, d=4:number, e=5:number
// test.kt:18 baz: param=6:number, b=2:number, d=4:number, e=5:number, g=7:number, d=4:number, e=5:number, g=7:number, d=4:number, e=5:number, g=7:number
// test.kt:31 box:
