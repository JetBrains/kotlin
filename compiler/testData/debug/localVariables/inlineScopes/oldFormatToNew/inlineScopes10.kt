// MODULE: library
// FILE: library.kt

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
    block()
}

// MODULE: test(library)
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    bar() {
        val d = 4
    }
}

// EXPECTATIONS JVM_IR
// test.kt:33 box:
// library.kt:5 box: $i$f$bar\1\33:int=0:int
// library.kt:5 <init>:
// library.kt:21 box: $i$f$bar\1\33:int=0:int
// library.kt:7 baz: param:int=6:int
// library.kt:8 baz: param:int=6:int, b:int=2:int
// test.kt:34 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\1\8\0:int=0:int
// test.kt:35 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\1\8\0:int=0:int, d\1:int=4:int
// library.kt:8 baz: param:int=6:int, b:int=2:int
// library.kt:9 baz: param:int=6:int, b:int=2:int
// library.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\9:int=0:int
// library.kt:10 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\9:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$1\3\31\0:int=0:int
// library.kt:11 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\9:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$1\3\31\0:int=0:int, g\3:int=7:int
// library.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\9:int=0:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\2\9:int=0:int
// library.kt:12 baz: param:int=6:int, b:int=2:int
// test.kt:34 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\4\12\0:int=0:int
// test.kt:35 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\4\12\0:int=0:int, d\4:int=4:int
// library.kt:12 baz: param:int=6:int, b:int=2:int
// library.kt:13 baz: param:int=6:int, b:int=2:int
// library.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\13:int=0:int
// library.kt:14 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\13:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$2\6\33\0:int=0:int
// library.kt:15 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\13:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$2\6\33\0:int=0:int, g\6:int=7:int
// library.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\13:int=0:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\13:int=0:int
// library.kt:16 baz: param:int=6:int, b:int=2:int
// test.kt:34 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\7\16\0:int=0:int
// test.kt:35 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\7\16\0:int=0:int, d\7:int=4:int
// library.kt:16 baz: param:int=6:int, b:int=2:int
// library.kt:17 baz: param:int=6:int, b:int=2:int
// library.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\17:int=0:int
// library.kt:18 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\17:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$3\9\35\0:int=0:int
// library.kt:19 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\17:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$3\9\35\0:int=0:int, g\9:int=7:int
// library.kt:25 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\17:int=0:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\8\17:int=0:int
// library.kt:20 baz: param:int=6:int, b:int=2:int
// library.kt:22 box: $i$f$bar\1\33:int=0:int
// test.kt:36 box:

// EXPECTATIONS WASM
// test.kt:33 $box: (4)
// library.kt:5 $box: (4)
// library.kt:21 $<no name provided>.<init>: $<this>:(ref $<no name provided>)=(ref $<no name provided>) (5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
// library.kt:21 $box: (10, 6)
// library.kt:7 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=0:i32, $d:i32=0:i32, $g:i32=0:i32 (20, 20)
// library.kt:8 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=0:i32, $g:i32=0:i32 (12)
// test.kt:34 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=0:i32 (16, 16, 16)
// test.kt:35 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=0:i32 (5, 5)
// library.kt:9 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=0:i32 (12)
// library.kt:25 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=0:i32 (4)
// library.kt:10 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (24, 24, 24)
// library.kt:11 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (13, 13, 13)
// library.kt:26 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (1, 1)
// library.kt:12 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (12)
// test.kt:34 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (16, 16, 16)
// test.kt:35 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (5, 5)
// library.kt:13 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (12)
// library.kt:25 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (4)
// library.kt:14 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (24, 24, 24)
// library.kt:15 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (13, 13, 13)
// library.kt:26 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (1, 1)
// library.kt:16 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (12)
// test.kt:34 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (16, 16, 16)
// test.kt:35 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (5, 5)
// library.kt:17 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (12)
// library.kt:25 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (4)
// library.kt:18 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (24, 24, 24)
// library.kt:19 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (13, 13, 13)
// library.kt:26 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (1, 1)
// library.kt:20 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $g:i32=7:i32 (9, 9)
// library.kt:21 $box: (6)
// library.kt:22 $box: (1)
// test.kt:36 $box: (1)
