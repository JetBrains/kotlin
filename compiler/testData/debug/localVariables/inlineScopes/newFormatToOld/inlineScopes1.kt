// MODULE: library
// USE_INLINE_SCOPES_NUMBERS
// FILE: library.kt

inline fun foo(fooParam: Int, block: (Int) -> Unit) {
    var fooVar = 0
    block(42)
}

inline fun flaf() {
    var flafVar = 0
    foo(0) {
        val x = 1
        foo(1) {
            val y = 2
        }
        foo(2) {
            val z = 3
        }
    }
}

// MODULE: test(library)
// FILE: test.kt

fun box() {
    flaf()
}

// EXPECTATIONS JVM_IR
// test.kt:27 box:
// library.kt:11 box: $i$f$flaf:int=0:int
// library.kt:12 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int
// library.kt:6 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int
// library.kt:7 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int
// library.kt:13 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int
// library.kt:14 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int
// library.kt:6 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\3$iv:int=1:int, $i$f$foo\3\14:int=0:int
// library.kt:7 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\3$iv:int=1:int, $i$f$foo\3\14:int=0:int, fooVar\3$iv:int=0:int
// library.kt:15 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\3$iv:int=1:int, $i$f$foo\3\14:int=0:int, fooVar\3$iv:int=0:int, it\4$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1$1\4\27\2$iv:int=0:int
// library.kt:16 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\3$iv:int=1:int, $i$f$foo\3\14:int=0:int, fooVar\3$iv:int=0:int, it\4$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1$1\4\27\2$iv:int=0:int, y\4$iv:int=2:int
// library.kt:7 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\3$iv:int=1:int, $i$f$foo\3\14:int=0:int, fooVar\3$iv:int=0:int
// library.kt:8 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\3$iv:int=1:int, $i$f$foo\3\14:int=0:int, fooVar\3$iv:int=0:int
// library.kt:17 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int
// library.kt:6 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\5$iv:int=2:int, $i$f$foo\5\17:int=0:int
// library.kt:7 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\5$iv:int=2:int, $i$f$foo\5\17:int=0:int, fooVar\5$iv:int=0:int
// library.kt:18 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\5$iv:int=2:int, $i$f$foo\5\17:int=0:int, fooVar\5$iv:int=0:int, it\6$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1$2\6\30\2$iv:int=0:int
// library.kt:19 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\5$iv:int=2:int, $i$f$foo\5\17:int=0:int, fooVar\5$iv:int=0:int, it\6$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1$2\6\30\2$iv:int=0:int, z\6$iv:int=3:int
// library.kt:7 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\5$iv:int=2:int, $i$f$foo\5\17:int=0:int, fooVar\5$iv:int=0:int
// library.kt:8 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int, fooParam\5$iv:int=2:int, $i$f$foo\5\17:int=0:int, fooVar\5$iv:int=0:int
// library.kt:20 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int, it\2$iv:int=42:int, $i$a$-foo-LibraryKt$flaf$1\2\25\0$iv:int=0:int, x\2$iv:int=1:int
// library.kt:7 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int
// library.kt:8 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int, fooParam\1$iv:int=0:int, $i$f$foo\1\12:int=0:int, fooVar\1$iv:int=0:int
// library.kt:21 box: $i$f$flaf:int=0:int, flafVar$iv:int=0:int
// test.kt:28 box:
