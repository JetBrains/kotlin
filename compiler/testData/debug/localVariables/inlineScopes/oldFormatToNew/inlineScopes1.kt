// MODULE: library
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
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    flaf()
}

// EXPECTATIONS JVM_IR
// test.kt:27 box:
// library.kt:10 box: $i$f$flaf\1\27:int=0:int
// library.kt:11 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int
// library.kt:5 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int
// library.kt:12 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int
// library.kt:13 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int
// library.kt:5 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\67:int=0:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\67:int=0:int, fooVar\4:int=0:int
// library.kt:14 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\67:int=0:int, fooVar\4:int=0:int, it\5:int=42:int, $i$a$-foo-LibraryKt$flaf$1$1\5\60\3:int=0:int
// library.kt:15 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\67:int=0:int, fooVar\4:int=0:int, it\5:int=42:int, $i$a$-foo-LibraryKt$flaf$1$1\5\60\3:int=0:int, y\5:int=2:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\67:int=0:int, fooVar\4:int=0:int
// library.kt:7 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\67:int=0:int, fooVar\4:int=0:int
// library.kt:16 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int
// library.kt:5 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\70:int=0:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\70:int=0:int, fooVar\6:int=0:int
// library.kt:17 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\70:int=0:int, fooVar\6:int=0:int, it\7:int=42:int, $i$a$-foo-LibraryKt$flaf$1$2\7\60\3:int=0:int
// library.kt:18 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\70:int=0:int, fooVar\6:int=0:int, it\7:int=42:int, $i$a$-foo-LibraryKt$flaf$1$2\7\60\3:int=0:int, z\7:int=3:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\70:int=0:int, fooVar\6:int=0:int
// library.kt:7 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\70:int=0:int, fooVar\6:int=0:int
// library.kt:19 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\60\1:int=0:int, x\3:int=1:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int
// library.kt:7 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\58:int=0:int, fooVar\2:int=0:int
// library.kt:20 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int
// test.kt:28 box:
