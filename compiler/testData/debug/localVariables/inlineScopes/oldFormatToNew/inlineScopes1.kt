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
// library.kt:5 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int
// library.kt:12 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int
// library.kt:13 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int
// library.kt:5 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\91:int=0:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\91:int=0:int, fooVar\4:int=0:int
// library.kt:14 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\91:int=0:int, fooVar\4:int=0:int, it\5:int=42:int, $i$a$-foo-LibraryKt$flaf$1$1\5\84\3:int=0:int
// library.kt:15 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\91:int=0:int, fooVar\4:int=0:int, it\5:int=42:int, $i$a$-foo-LibraryKt$flaf$1$1\5\84\3:int=0:int, y\5:int=2:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\91:int=0:int, fooVar\4:int=0:int
// library.kt:7 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\4:int=1:int, $i$f$foo\4\91:int=0:int, fooVar\4:int=0:int
// library.kt:16 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int
// library.kt:5 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\94:int=0:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\94:int=0:int, fooVar\6:int=0:int
// library.kt:17 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\94:int=0:int, fooVar\6:int=0:int, it\7:int=42:int, $i$a$-foo-LibraryKt$flaf$1$2\7\84\3:int=0:int
// library.kt:18 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\94:int=0:int, fooVar\6:int=0:int, it\7:int=42:int, $i$a$-foo-LibraryKt$flaf$1$2\7\84\3:int=0:int, z\7:int=3:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\94:int=0:int, fooVar\6:int=0:int
// library.kt:7 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int, fooParam\6:int=2:int, $i$f$foo\6\94:int=0:int, fooVar\6:int=0:int
// library.kt:19 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int, it\3:int=42:int, $i$a$-foo-LibraryKt$flaf$1\3\84\1:int=0:int, x\3:int=1:int
// library.kt:6 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int
// library.kt:7 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int, fooParam\2:int=0:int, $i$f$foo\2\82:int=0:int, fooVar\2:int=0:int
// library.kt:20 box: $i$f$flaf\1\27:int=0:int, flafVar\1:int=0:int
// test.kt:28 box:

// EXPECTATIONS WASM
// test.kt:27 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=0:i32, $y:i32=0:i32, $z:i32=0:i32 (4)
// library.kt:10 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=0:i32, $y:i32=0:i32, $z:i32=0:i32 (18, 18)
// library.kt:11 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=0:i32, $y:i32=0:i32, $z:i32=0:i32 (8, 8, 4, 4, 4)
// library.kt:5 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=0:i32, $y:i32=0:i32, $z:i32=0:i32 (17, 17)
// library.kt:6 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=0:i32, $y:i32=0:i32, $z:i32=0:i32 (10, 10, 4, 4, 4)
// library.kt:12 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=0:i32, $y:i32=0:i32, $z:i32=0:i32 (16, 16)
// library.kt:13 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=0:i32, $z:i32=0:i32 (12, 12, 8, 8, 8)
// library.kt:5 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=0:i32, $z:i32=0:i32 (17, 17)
// library.kt:6 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=0:i32, $z:i32=0:i32 (10, 10, 4, 4, 4)
// library.kt:14 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=0:i32 (20, 20, 20)
// library.kt:15 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=0:i32 (9, 9, 9)
// library.kt:7 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=0:i32 (1, 1)
// library.kt:16 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=0:i32 (12, 12, 8, 8, 8)
// library.kt:5 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=0:i32 (17, 17)
// library.kt:6 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=0:i32 (10, 10, 4, 4, 4)
// library.kt:17 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=3:i32 (20, 20, 20)
// library.kt:18 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=3:i32 (9, 9, 9)
// library.kt:7 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=3:i32 (1, 1, 1)
// library.kt:19 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=3:i32 (5, 5, 5)
// library.kt:7 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=3:i32 (1, 1, 1)
// library.kt:20 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=3:i32 (1, 1)
// test.kt:28 $box: $flafVar:i32=0:i32, $fooVar:i32=0:i32, $x:i32=1:i32, $y:i32=2:i32, $z:i32=3:i32 (1, 1)
