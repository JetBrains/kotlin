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

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:20 box:
// test.kt:3 box: $i$f$foo\1\20:int=0:int
// test.kt:3 <init>:
// test.kt:7 box: $i$f$foo\1\20:int=0:int
// test.kt:5 baz: param:int=5:int
// test.kt:6 baz: param:int=5:int, a:int=1:int
// test.kt:8 box: $i$f$foo\1\20:int=0:int
// test.kt:24 box:
// test.kt:11 box: $i$f$bar\2\24:int=0:int
// test.kt:11 <init>:
// test.kt:16 box: $i$f$bar\2\24:int=0:int
// test.kt:13 baz: param:int=6:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$2\1\14\0:int=0:int
// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:26 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$2\1\14\0:int=0:int, d\1:int=4:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:15 baz: param:int=6:int, b:int=2:int
// test.kt:17 box: $i$f$bar\2\24:int=0:int
// test.kt:27 box:


// EXPECTATIONS JVM_IR
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
// test.kt:26 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$2:int=0:int, d:int=4:int
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

// EXPECTATIONS WASM
// test.kt:20 $box: (4)
// test.kt:3 $box: (4)
// test.kt:7 $<no name provided>.<init>: $<this>:(ref $<no name provided>)=(ref $<no name provided>) (5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
// test.kt:7 $box: (10, 6)
// test.kt:5 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=5:i32, $a:i32=0:i32 (20, 20)
// test.kt:6 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=5:i32, $a:i32=1:i32 (9, 9)
// test.kt:7 $box: (6)
// test.kt:8 $box: (1)
// test.kt:24 $box: (4)
// test.kt:11 $box: (4)
// test.kt:16 $<no name provided>.<init>: $<this>:(ref $<no name provided>)=(ref $<no name provided>) (5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
// test.kt:16 $box: (10, 6)
// test.kt:13 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=0:i32, $d:i32=0:i32 (20, 20)
// test.kt:14 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=0:i32 (12)
// test.kt:25 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32 (16, 16, 16)
// test.kt:26 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32 (5, 5)
// test.kt:15 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32 (9, 9)
// test.kt:16 $box: (6)
// test.kt:17 $box: (1)
// test.kt:27 $box: (1)
