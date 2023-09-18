// MODULE: library
// USE_INLINE_SCOPES_NUMBERS
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
// FILE: test.kt

fun box() {
    bar() {
        val d = 4
    }
}

// EXPECTATIONS JVM_IR
// test.kt:33 box:
// library.kt:6 box: $i$f$bar:int=0:int
// library.kt:6 <init>:
// library.kt:22 box: $i$f$bar:int=0:int
// library.kt:8 baz: param:int=6:int
// library.kt:9 baz: param:int=6:int, b:int=2:int
// test.kt:34 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:35 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int, d:int=4:int
// library.kt:9 baz: param:int=6:int, b:int=2:int
// library.kt:10 baz: param:int=6:int, b:int=2:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\1\10:int=0:int
// library.kt:11 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\1\10:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$1\2\30\0:int=0:int
// library.kt:12 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\1\10:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$1\2\30\0:int=0:int, g\2:int=7:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\1\10:int=0:int
// library.kt:27 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\1\10:int=0:int
// library.kt:13 baz: param:int=6:int, b:int=2:int
// test.kt:34 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:35 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int, d:int=4:int
// library.kt:13 baz: param:int=6:int, b:int=2:int
// library.kt:14 baz: param:int=6:int, b:int=2:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\3\14:int=0:int
// library.kt:15 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\3\14:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$2\4\32\0:int=0:int
// library.kt:16 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\3\14:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$2\4\32\0:int=0:int, g\4:int=7:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\3\14:int=0:int
// library.kt:27 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\3\14:int=0:int
// library.kt:17 baz: param:int=6:int, b:int=2:int
// test.kt:34 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:35 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int, d:int=4:int
// library.kt:17 baz: param:int=6:int, b:int=2:int
// library.kt:18 baz: param:int=6:int, b:int=2:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\18:int=0:int
// library.kt:19 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\18:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$3\6\34\0:int=0:int
// library.kt:20 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\18:int=0:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$3\6\34\0:int=0:int, g\6:int=7:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\18:int=0:int
// library.kt:27 baz: param:int=6:int, b:int=2:int, $i$f$inlineCall\5\18:int=0:int
// library.kt:21 baz: param:int=6:int, b:int=2:int
// library.kt:23 box: $i$f$bar:int=0:int
// test.kt:36 box:
