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
// test.kt:8 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int, e\2:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1\3\177\0:int=0:int
// test.kt:9 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int, e\2:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$1\3\177\0:int=0:int, g\3:int=7:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int, e\2:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\7:int=0:int, e\2:int=5:int
// test.kt:10 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\4\10\0:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\4\10\0:int=0:int, d\4:int=4:int
// test.kt:10 baz: param:int=6:int, b:int=2:int
// test.kt:11 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int
// test.kt:12 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$2\6\180\0:int=0:int
// test.kt:13 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$2\6\180\0:int=0:int, g\6:int=7:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int
// test.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\11:int=0:int, e\5:int=5:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:29 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\7\14\0:int=0:int
// test.kt:30 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\7\14\0:int=0:int, d\7:int=4:int
// test.kt:14 baz: param:int=6:int, b:int=2:int
// test.kt:15 baz: param:int=6:int, b:int=2:int
// test.kt:23 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int
// test.kt:24 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int, e\8:int=5:int
// test.kt:16 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int, e\8:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$3\9\183\0:int=0:int
// test.kt:17 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\15:int=0:int, e\8:int=5:int, $i$a$-inlineCall-TestKt$bar$1$baz$3\9\183\0:int=0:int, g\9:int=7:int
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

// EXPECTATIONS WASM
// test.kt:28 $box: (4)
// test.kt:3 $box: (4)
// test.kt:19 $<no name provided>.<init>: $<this>:(ref $<no name provided>)=(ref $<no name provided>) (5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
// test.kt:19 $box: (10, 6)
// test.kt:5 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=0:i32, $d:i32=0:i32, $e:i32=0:i32, $g:i32=0:i32 (20, 20)
// test.kt:6 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=0:i32, $e:i32=0:i32, $g:i32=0:i32 (12)
// test.kt:29 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=0:i32, $g:i32=0:i32 (16, 16, 16)
// test.kt:30 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=0:i32, $g:i32=0:i32 (5, 5)
// test.kt:7 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=0:i32, $g:i32=0:i32 (12)
// test.kt:23 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=0:i32, $g:i32=0:i32 (12, 12)
// test.kt:24 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=0:i32 (4)
// test.kt:8 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (24, 24, 24)
// test.kt:9 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (13, 13, 13)
// test.kt:25 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (1, 1)
// test.kt:10 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (12)
// test.kt:29 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (16, 16, 16)
// test.kt:30 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (5, 5)
// test.kt:11 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (12)
// test.kt:23 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (12, 12)
// test.kt:24 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (4)
// test.kt:12 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (24, 24, 24)
// test.kt:13 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (13, 13, 13)
// test.kt:25 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (1, 1)
// test.kt:14 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (12)
// test.kt:29 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (16, 16, 16)
// test.kt:30 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (5, 5)
// test.kt:15 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (12)
// test.kt:23 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (12, 12)
// test.kt:24 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (4)
// test.kt:16 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (24, 24, 24)
// test.kt:17 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (13, 13, 13)
// test.kt:25 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (1, 1)
// test.kt:18 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $g:i32=7:i32 (9, 9)
// test.kt:19 $box: (6)
// test.kt:20 $box: (1)
// test.kt:31 $box: (1)
