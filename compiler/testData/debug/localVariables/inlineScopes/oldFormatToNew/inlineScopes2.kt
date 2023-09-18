// MODULE: library
// FILE: library.kt

inline fun foo(fooParam: Int, block: (Int) -> Unit) {
    val fooVar = 1
    baz(1)
    block(1)
}

inline fun bar(barParam: Int, block: (Int) -> Unit) {
    val barVar = 2
    block(1)
}

inline fun baz(bazParam: Int) {
    val bazVar = 3
    baz1(1) { baz1BlockParam ->
        val baz1LambdaVar = 1
        baz1(2) { baz1BlockParam1 ->
            val baz1LambdaVar1 = 1
            3
        }
        2
    }
}

inline fun baz1(baz1Param: Int, block: (Int) -> Int) {
    val baz1Var = 3
    baz2(1)
    block(1)
}

inline fun baz2(baz2Param: Int) {
    val baz2Var = 3
}

inline fun flaf() {
    val flafVar = 1
    foo(1) { fooLambdaParam ->
        val fooLamdbdaVar = 2
        bar(2) { barLambdaParam ->
            val barLamdbdaVar = 3
        }
    }
}

// MODULE: test(library)
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    val mainVar = 1
    flaf()
    foo(1) { fooLambdaParam ->
        val fooLamdbdaVar = 2
        bar(2) { barLambdaParam ->
            val barLamdbdaVar = 3
        }
    }
}

// EXPECTATIONS JVM_IR
// test.kt:52 box:
// test.kt:53 box: mainVar:int=1:int
// library.kt:38 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int
// library.kt:39 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int
// library.kt:5 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int
// library.kt:6 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int
// library.kt:16 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int
// library.kt:17 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int
// library.kt:28 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int
// library.kt:29 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int
// library.kt:34 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz2Param\5:int=1:int, $i$f$baz2\5\159:int=0:int
// library.kt:35 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz2Param\5:int=1:int, $i$f$baz2\5\159:int=0:int, baz2Var\5:int=3:int
// library.kt:30 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int
// library.kt:18 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int
// library.kt:19 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int
// library.kt:28 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int, baz1Param\7:int=2:int, $i$f$baz1\7\167:int=0:int
// library.kt:29 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int, baz1Param\7:int=2:int, $i$f$baz1\7\167:int=0:int, baz1Var\7:int=3:int
// library.kt:34 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int, baz1Param\7:int=2:int, $i$f$baz1\7\167:int=0:int, baz1Var\7:int=3:int, baz2Param\8:int=1:int, $i$f$baz2\8\177:int=0:int
// library.kt:35 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int, baz1Param\7:int=2:int, $i$f$baz1\7\167:int=0:int, baz1Var\7:int=3:int, baz2Param\8:int=1:int, $i$f$baz2\8\177:int=0:int, baz2Var\8:int=3:int
// library.kt:30 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int, baz1Param\7:int=2:int, $i$f$baz1\7\167:int=0:int, baz1Var\7:int=3:int
// library.kt:20 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int, baz1Param\7:int=2:int, $i$f$baz1\7\167:int=0:int, baz1Var\7:int=3:int, baz1BlockParam1\9:int=1:int, $i$a$-baz1-LibraryKt$baz$1$1\9\178\6:int=0:int
// library.kt:21 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int, baz1Param\7:int=2:int, $i$f$baz1\7\167:int=0:int, baz1Var\7:int=3:int, baz1BlockParam1\9:int=1:int, $i$a$-baz1-LibraryKt$baz$1$1\9\178\6:int=0:int, baz1LambdaVar1\9:int=1:int
// library.kt:30 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int, baz1Param\7:int=2:int, $i$f$baz1\7\167:int=0:int, baz1Var\7:int=3:int
// library.kt:31 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int, baz1Param\7:int=2:int, $i$f$baz1\7\167:int=0:int, baz1Var\7:int=3:int
// library.kt:23 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int, baz1BlockParam\6:int=1:int, $i$a$-baz1-LibraryKt$baz$1\6\160\3:int=0:int, baz1LambdaVar\6:int=1:int
// library.kt:30 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int
// library.kt:31 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int, baz1Param\4:int=1:int, $i$f$baz1\4\157:int=0:int, baz1Var\4:int=3:int
// library.kt:25 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, bazParam\3:int=1:int, $i$f$baz\3\146:int=0:int, bazVar\3:int=3:int
// library.kt:7 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int
// library.kt:40 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, fooLambdaParam\10:int=1:int, $i$a$-foo-LibraryKt$flaf$1\10\184\1:int=0:int
// library.kt:41 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, fooLambdaParam\10:int=1:int, $i$a$-foo-LibraryKt$flaf$1\10\184\1:int=0:int, fooLamdbdaVar\10:int=2:int
// library.kt:11 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, fooLambdaParam\10:int=1:int, $i$a$-foo-LibraryKt$flaf$1\10\184\1:int=0:int, fooLamdbdaVar\10:int=2:int, barParam\11:int=2:int, $i$f$bar\11\186:int=0:int
// library.kt:12 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, fooLambdaParam\10:int=1:int, $i$a$-foo-LibraryKt$flaf$1\10\184\1:int=0:int, fooLamdbdaVar\10:int=2:int, barParam\11:int=2:int, $i$f$bar\11\186:int=0:int, barVar\11:int=2:int
// library.kt:42 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, fooLambdaParam\10:int=1:int, $i$a$-foo-LibraryKt$flaf$1\10\184\1:int=0:int, fooLamdbdaVar\10:int=2:int, barParam\11:int=2:int, $i$f$bar\11\186:int=0:int, barVar\11:int=2:int, barLambdaParam\12:int=1:int, $i$a$-bar-LibraryKt$flaf$1$1\12\188\10:int=0:int
// library.kt:43 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, fooLambdaParam\10:int=1:int, $i$a$-foo-LibraryKt$flaf$1\10\184\1:int=0:int, fooLamdbdaVar\10:int=2:int, barParam\11:int=2:int, $i$f$bar\11\186:int=0:int, barVar\11:int=2:int, barLambdaParam\12:int=1:int, $i$a$-bar-LibraryKt$flaf$1$1\12\188\10:int=0:int, barLamdbdaVar\12:int=3:int
// library.kt:12 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, fooLambdaParam\10:int=1:int, $i$a$-foo-LibraryKt$flaf$1\10\184\1:int=0:int, fooLamdbdaVar\10:int=2:int, barParam\11:int=2:int, $i$f$bar\11\186:int=0:int, barVar\11:int=2:int
// library.kt:13 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, fooLambdaParam\10:int=1:int, $i$a$-foo-LibraryKt$flaf$1\10\184\1:int=0:int, fooLamdbdaVar\10:int=2:int, barParam\11:int=2:int, $i$f$bar\11\186:int=0:int, barVar\11:int=2:int
// library.kt:44 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int, fooLambdaParam\10:int=1:int, $i$a$-foo-LibraryKt$flaf$1\10\184\1:int=0:int, fooLamdbdaVar\10:int=2:int
// library.kt:7 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int
// library.kt:8 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int, fooParam\2:int=1:int, $i$f$foo\2\144:int=0:int, fooVar\2:int=1:int
// library.kt:45 box: mainVar:int=1:int, $i$f$flaf\1\53:int=0:int, flafVar\1:int=1:int
// test.kt:54 box: mainVar:int=1:int
// library.kt:5 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int
// library.kt:6 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int
// library.kt:16 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int
// library.kt:17 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int
// library.kt:28 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int
// library.kt:29 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int
// library.kt:34 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz2Param\16:int=1:int, $i$f$baz2\16\209:int=0:int
// library.kt:35 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz2Param\16:int=1:int, $i$f$baz2\16\209:int=0:int, baz2Var\16:int=3:int
// library.kt:30 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int
// library.kt:18 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int
// library.kt:19 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int
// library.kt:28 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int, baz1Param\18:int=2:int, $i$f$baz1\18\217:int=0:int
// library.kt:29 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int, baz1Param\18:int=2:int, $i$f$baz1\18\217:int=0:int, baz1Var\18:int=3:int
// library.kt:34 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int, baz1Param\18:int=2:int, $i$f$baz1\18\217:int=0:int, baz1Var\18:int=3:int, baz2Param\19:int=1:int, $i$f$baz2\19\227:int=0:int
// library.kt:35 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int, baz1Param\18:int=2:int, $i$f$baz1\18\217:int=0:int, baz1Var\18:int=3:int, baz2Param\19:int=1:int, $i$f$baz2\19\227:int=0:int, baz2Var\19:int=3:int
// library.kt:30 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int, baz1Param\18:int=2:int, $i$f$baz1\18\217:int=0:int, baz1Var\18:int=3:int
// library.kt:20 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int, baz1Param\18:int=2:int, $i$f$baz1\18\217:int=0:int, baz1Var\18:int=3:int, baz1BlockParam1\20:int=1:int, $i$a$-baz1-LibraryKt$baz$1$1\20\228\17:int=0:int
// library.kt:21 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int, baz1Param\18:int=2:int, $i$f$baz1\18\217:int=0:int, baz1Var\18:int=3:int, baz1BlockParam1\20:int=1:int, $i$a$-baz1-LibraryKt$baz$1$1\20\228\17:int=0:int, baz1LambdaVar1\20:int=1:int
// library.kt:30 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int, baz1Param\18:int=2:int, $i$f$baz1\18\217:int=0:int, baz1Var\18:int=3:int
// library.kt:31 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int, baz1Param\18:int=2:int, $i$f$baz1\18\217:int=0:int, baz1Var\18:int=3:int
// library.kt:23 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int, baz1BlockParam\17:int=1:int, $i$a$-baz1-LibraryKt$baz$1\17\210\14:int=0:int, baz1LambdaVar\17:int=1:int
// library.kt:30 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int
// library.kt:31 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int, baz1Param\15:int=1:int, $i$f$baz1\15\207:int=0:int, baz1Var\15:int=3:int
// library.kt:25 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, bazParam\14:int=1:int, $i$f$baz\14\196:int=0:int, bazVar\14:int=3:int
// library.kt:7 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int
// test.kt:55 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, fooLambdaParam\21:int=1:int, $i$a$-foo-TestKt$box$1\21\234\0:int=0:int
// test.kt:56 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, fooLambdaParam\21:int=1:int, $i$a$-foo-TestKt$box$1\21\234\0:int=0:int, fooLamdbdaVar\21:int=2:int
// library.kt:11 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, fooLambdaParam\21:int=1:int, $i$a$-foo-TestKt$box$1\21\234\0:int=0:int, fooLamdbdaVar\21:int=2:int, barParam\22:int=2:int, $i$f$bar\22\56:int=0:int
// library.kt:12 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, fooLambdaParam\21:int=1:int, $i$a$-foo-TestKt$box$1\21\234\0:int=0:int, fooLamdbdaVar\21:int=2:int, barParam\22:int=2:int, $i$f$bar\22\56:int=0:int, barVar\22:int=2:int
// test.kt:57 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, fooLambdaParam\21:int=1:int, $i$a$-foo-TestKt$box$1\21\234\0:int=0:int, fooLamdbdaVar\21:int=2:int, barParam\22:int=2:int, $i$f$bar\22\56:int=0:int, barVar\22:int=2:int, barLambdaParam\23:int=1:int, $i$a$-bar-TestKt$box$1$1\23\236\21:int=0:int
// test.kt:58 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, fooLambdaParam\21:int=1:int, $i$a$-foo-TestKt$box$1\21\234\0:int=0:int, fooLamdbdaVar\21:int=2:int, barParam\22:int=2:int, $i$f$bar\22\56:int=0:int, barVar\22:int=2:int, barLambdaParam\23:int=1:int, $i$a$-bar-TestKt$box$1$1\23\236\21:int=0:int, barLamdbdaVar\23:int=3:int
// library.kt:12 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, fooLambdaParam\21:int=1:int, $i$a$-foo-TestKt$box$1\21\234\0:int=0:int, fooLamdbdaVar\21:int=2:int, barParam\22:int=2:int, $i$f$bar\22\56:int=0:int, barVar\22:int=2:int
// library.kt:13 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, fooLambdaParam\21:int=1:int, $i$a$-foo-TestKt$box$1\21\234\0:int=0:int, fooLamdbdaVar\21:int=2:int, barParam\22:int=2:int, $i$f$bar\22\56:int=0:int, barVar\22:int=2:int
// test.kt:59 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int, fooLambdaParam\21:int=1:int, $i$a$-foo-TestKt$box$1\21\234\0:int=0:int, fooLamdbdaVar\21:int=2:int
// library.kt:7 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int
// library.kt:8 box: mainVar:int=1:int, fooParam\13:int=1:int, $i$f$foo\13\54:int=0:int, fooVar\13:int=1:int
// test.kt:60 box: mainVar:int=1:int
