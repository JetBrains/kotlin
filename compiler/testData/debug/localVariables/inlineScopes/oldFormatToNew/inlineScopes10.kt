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
