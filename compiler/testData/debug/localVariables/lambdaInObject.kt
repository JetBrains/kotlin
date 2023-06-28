// FILE: test.kt
inline fun foo(block: () -> Unit) {
    object {
        fun baz(param: Int) {
            val a = 1
        }
    }.baz(5)
}

inline fun bar(crossinline block: () -> Unit) {
    object {
        fun baz(param: Int) {
            val b = 2
            block()
        }
    }.baz(6)
}

fun box() {
    foo() {
        val c = 3
    }

    bar() {
        val d = 4
    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:20 box:
// test.kt:3 box: $i$f$foo:int=0:int
// test.kt:3 <init>:
// test.kt:7 box: $i$f$foo:int=0:int
// test.kt:5 baz: param:int=5:int
// test.kt:6 baz: param:int=5:int, a:int=1:int
// test.kt:8 box: $i$f$foo:int=0:int
// test.kt:24 box:
// test.kt:11 box: $i$f$bar:int=0:int
// test.kt:11 <init>:
// test.kt:16 box: $i$f$bar:int=0:int
// test.kt:13 baz: param:int=6:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$2:int=0:int
// EXPECTATIONS JVM_IR
// test.kt:26 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$2:int=0:int, d:int=4:int
// EXPECTATIONS JVM
// test.kt:26 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$2:int=0:int
// EXPECTATIONS JVM JVM_IR
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:15 baz: param:int=6:int, b:int=2:int
// test.kt:17 box: $i$f$bar:int=0:int
// test.kt:27 box:

// EXPECTATIONS JS_IR
// test.kt:7 box:
// test.kt:3 <init>:
// test.kt:7 box:
// test.kt:5 baz: param=5:number
// test.kt:6 baz: param=5:number, a=1:number
// test.kt:16 box:
// test.kt:11 <init>:
// test.kt:16 box:
// test.kt:13 baz: param=6:number
// test.kt:25 baz: param=6:number, b=2:number
// test.kt:15 baz: param=6:number, b=2:number, d=4:number
// test.kt:27 box:
