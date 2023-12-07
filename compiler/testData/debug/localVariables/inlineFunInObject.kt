// FILE: test.kt
inline fun foo(block: () -> Unit) {
    object {
        fun baz(param: Int) {
            val a = 1
            inlineCall {
                val f = 6
            }
        }
    }.baz(5)
}

inline fun bar(crossinline block: () -> Unit) {
    object {
        fun baz(param: Int) {
            val b = 2
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
    foo() {
        val c = 3
    }

    bar() {
        val d = 4
    }
}

// EXPECTATIONS JVM_IR
// test.kt:31 box:
// test.kt:3 box: $i$f$foo:int=0:int
// test.kt:3 <init>:
// test.kt:10 box: $i$f$foo:int=0:int
// test.kt:5 baz: param:int=5:int
// test.kt:6 baz: param:int=5:int, a:int=1:int
// test.kt:26 baz: param:int=5:int, a:int=1:int, $i$f$inlineCall:int=0:int
// test.kt:27 baz: param:int=5:int, a:int=1:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:7 baz: param:int=5:int, a:int=1:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$foo$1$baz$1:int=0:int
// test.kt:8 baz: param:int=5:int, a:int=1:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$foo$1$baz$1:int=0:int, f:int=6:int
// test.kt:27 baz: param:int=5:int, a:int=1:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:28 baz: param:int=5:int, a:int=1:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:9 baz: param:int=5:int, a:int=1:int
// test.kt:11 box: $i$f$foo:int=0:int
// test.kt:35 box:
// test.kt:14 box: $i$f$bar:int=0:int
// test.kt:14 <init>:
// test.kt:22 box: $i$f$bar:int=0:int
// test.kt:16 baz: param:int=6:int
// test.kt:17 baz: param:int=6:int, b:int=2:int
// test.kt:36 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$2:int=0:int
// test.kt:37 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$2:int=0:int, d:int=4:int
// test.kt:17 baz: param:int=6:int, b:int=2:int
// test.kt:18 baz: param:int=6:int, b:int=2:int
// test.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int
// test.kt:27 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:19 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1:int=0:int
// test.kt:20 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1:int=0:int, g:int=7:int
// test.kt:27 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:28 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall:int=0:int, e$iv:int=5:int
// test.kt:21 baz: param:int=6:int, b:int=2:int
// test.kt:23 box: $i$f$bar:int=0:int
// test.kt:38 box:

// EXPECTATIONS JS_IR
// test.kt:10 box:
// test.kt:3 <init>:
// test.kt:10 box:
// test.kt:5 baz: param=5:number
// test.kt:26 baz: param=5:number, a=1:number
// test.kt:7 baz: param=5:number, a=1:number, e=5:number
// test.kt:9 baz: param=5:number, a=1:number, e=5:number, f=6:number
// test.kt:22 box:
// test.kt:14 <init>:
// test.kt:22 box:
// test.kt:16 baz: param=6:number
// test.kt:36 baz: param=6:number, b=2:number
// test.kt:26 baz: param=6:number, b=2:number, d=4:number
// test.kt:19 baz: param=6:number, b=2:number, d=4:number, e=5:number
// test.kt:21 baz: param=6:number, b=2:number, d=4:number, e=5:number, g=7:number
// test.kt:38 box:
