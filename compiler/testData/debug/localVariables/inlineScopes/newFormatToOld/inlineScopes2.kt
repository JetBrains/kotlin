// MODULE: library
// USE_INLINE_SCOPES_NUMBERS
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
// library.kt:39 box: mainVar:int=1:int, $i$f$flaf:int=0:int
// library.kt:40 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int
// library.kt:6 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int
// library.kt:7 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int
// library.kt:17 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int
// library.kt:18 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int
// library.kt:29 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int
// library.kt:30 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int
// library.kt:35 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz2Param\4$iv:int=1:int, $i$f$baz2\4\110:int=0:int
// library.kt:36 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz2Param\4$iv:int=1:int, $i$f$baz2\4\110:int=0:int, baz2Var\4$iv:int=3:int
// library.kt:31 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int
// library.kt:19 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int
// library.kt:20 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int
// library.kt:29 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int, baz1Param\6$iv:int=2:int, $i$f$baz1\6\118:int=0:int
// library.kt:30 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int, baz1Param\6$iv:int=2:int, $i$f$baz1\6\118:int=0:int, baz1Var\6$iv:int=3:int
// library.kt:35 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int, baz1Param\6$iv:int=2:int, $i$f$baz1\6\118:int=0:int, baz1Var\6$iv:int=3:int, baz2Param\7$iv:int=1:int, $i$f$baz2\7\128:int=0:int
// library.kt:36 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int, baz1Param\6$iv:int=2:int, $i$f$baz1\6\118:int=0:int, baz1Var\6$iv:int=3:int, baz2Param\7$iv:int=1:int, $i$f$baz2\7\128:int=0:int, baz2Var\7$iv:int=3:int
// library.kt:31 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int, baz1Param\6$iv:int=2:int, $i$f$baz1\6\118:int=0:int, baz1Var\6$iv:int=3:int
// library.kt:21 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int, baz1Param\6$iv:int=2:int, $i$f$baz1\6\118:int=0:int, baz1Var\6$iv:int=3:int, baz1BlockParam1\8$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1$1\8\129\5$iv:int=0:int
// library.kt:22 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int, baz1Param\6$iv:int=2:int, $i$f$baz1\6\118:int=0:int, baz1Var\6$iv:int=3:int, baz1BlockParam1\8$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1$1\8\129\5$iv:int=0:int, baz1LambdaVar1\8$iv:int=1:int
// library.kt:31 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int, baz1Param\6$iv:int=2:int, $i$f$baz1\6\118:int=0:int, baz1Var\6$iv:int=3:int
// library.kt:32 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int, baz1Param\6$iv:int=2:int, $i$f$baz1\6\118:int=0:int, baz1Var\6$iv:int=3:int
// library.kt:24 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int, baz1BlockParam\5$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\5\111\2$iv:int=0:int, baz1LambdaVar\5$iv:int=1:int
// library.kt:31 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int
// library.kt:32 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int, baz1Param\3$iv:int=1:int, $i$f$baz1\3\108:int=0:int, baz1Var\3$iv:int=3:int
// library.kt:26 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, bazParam\2$iv:int=1:int, $i$f$baz\2\97:int=0:int, bazVar\2$iv:int=3:int
// library.kt:8 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int
// library.kt:41 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, fooLambdaParam\9$iv:int=1:int, $i$a$-foo-LibraryKt$flaf$1\9\135\0$iv:int=0:int
// library.kt:42 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, fooLambdaParam\9$iv:int=1:int, $i$a$-foo-LibraryKt$flaf$1\9\135\0$iv:int=0:int, fooLamdbdaVar\9$iv:int=2:int
// library.kt:12 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, fooLambdaParam\9$iv:int=1:int, $i$a$-foo-LibraryKt$flaf$1\9\135\0$iv:int=0:int, fooLamdbdaVar\9$iv:int=2:int, barParam\10$iv:int=2:int, $i$f$bar\10\42:int=0:int
// library.kt:13 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, fooLambdaParam\9$iv:int=1:int, $i$a$-foo-LibraryKt$flaf$1\9\135\0$iv:int=0:int, fooLamdbdaVar\9$iv:int=2:int, barParam\10$iv:int=2:int, $i$f$bar\10\42:int=0:int, barVar\10$iv:int=2:int
// library.kt:43 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, fooLambdaParam\9$iv:int=1:int, $i$a$-foo-LibraryKt$flaf$1\9\135\0$iv:int=0:int, fooLamdbdaVar\9$iv:int=2:int, barParam\10$iv:int=2:int, $i$f$bar\10\42:int=0:int, barVar\10$iv:int=2:int, barLambdaParam\11$iv:int=1:int, $i$a$-bar-LibraryKt$flaf$1$1\11\137\9$iv:int=0:int
// library.kt:44 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, fooLambdaParam\9$iv:int=1:int, $i$a$-foo-LibraryKt$flaf$1\9\135\0$iv:int=0:int, fooLamdbdaVar\9$iv:int=2:int, barParam\10$iv:int=2:int, $i$f$bar\10\42:int=0:int, barVar\10$iv:int=2:int, barLambdaParam\11$iv:int=1:int, $i$a$-bar-LibraryKt$flaf$1$1\11\137\9$iv:int=0:int, barLamdbdaVar\11$iv:int=3:int
// library.kt:13 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, fooLambdaParam\9$iv:int=1:int, $i$a$-foo-LibraryKt$flaf$1\9\135\0$iv:int=0:int, fooLamdbdaVar\9$iv:int=2:int, barParam\10$iv:int=2:int, $i$f$bar\10\42:int=0:int, barVar\10$iv:int=2:int
// library.kt:14 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, fooLambdaParam\9$iv:int=1:int, $i$a$-foo-LibraryKt$flaf$1\9\135\0$iv:int=0:int, fooLamdbdaVar\9$iv:int=2:int, barParam\10$iv:int=2:int, $i$f$bar\10\42:int=0:int, barVar\10$iv:int=2:int
// library.kt:45 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int, fooLambdaParam\9$iv:int=1:int, $i$a$-foo-LibraryKt$flaf$1\9\135\0$iv:int=0:int, fooLamdbdaVar\9$iv:int=2:int
// library.kt:8 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int
// library.kt:9 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int, fooParam\1$iv:int=1:int, $i$f$foo\1\40:int=0:int, fooVar\1$iv:int=1:int
// library.kt:46 box: mainVar:int=1:int, $i$f$flaf:int=0:int, flafVar$iv:int=1:int
// test.kt:54 box: mainVar:int=1:int
// library.kt:6 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int
// library.kt:7 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int
// library.kt:17 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int
// library.kt:18 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int
// library.kt:29 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int
// library.kt:30 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int
// library.kt:35 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz2Param\3$iv:int=1:int, $i$f$baz2\3\52:int=0:int
// library.kt:36 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz2Param\3$iv:int=1:int, $i$f$baz2\3\52:int=0:int, baz2Var\3$iv:int=3:int
// library.kt:31 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int
// library.kt:19 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int
// library.kt:20 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int
// library.kt:29 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int, baz1Param\5$iv:int=2:int, $i$f$baz1\5\60:int=0:int
// library.kt:30 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int, baz1Param\5$iv:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5$iv:int=3:int
// library.kt:35 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int, baz1Param\5$iv:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5$iv:int=3:int, baz2Param\6$iv:int=1:int, $i$f$baz2\6\70:int=0:int
// library.kt:36 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int, baz1Param\5$iv:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5$iv:int=3:int, baz2Param\6$iv:int=1:int, $i$f$baz2\6\70:int=0:int, baz2Var\6$iv:int=3:int
// library.kt:31 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int, baz1Param\5$iv:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5$iv:int=3:int
// library.kt:21 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int, baz1Param\5$iv:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5$iv:int=3:int, baz1BlockParam1\7$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1$1\7\71\4$iv:int=0:int
// library.kt:22 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int, baz1Param\5$iv:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5$iv:int=3:int, baz1BlockParam1\7$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1$1\7\71\4$iv:int=0:int, baz1LambdaVar1\7$iv:int=1:int
// library.kt:31 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int, baz1Param\5$iv:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5$iv:int=3:int
// library.kt:32 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int, baz1Param\5$iv:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5$iv:int=3:int
// library.kt:24 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int, baz1BlockParam\4$iv:int=1:int, $i$a$-baz1-LibraryKt$baz$1\4\53\1$iv:int=0:int, baz1LambdaVar\4$iv:int=1:int
// library.kt:31 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int
// library.kt:32 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int, baz1Param\2$iv:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2$iv:int=3:int
// library.kt:26 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, bazParam\1$iv:int=1:int, $i$f$baz\1\7:int=0:int, bazVar\1$iv:int=3:int
// library.kt:8 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int
// test.kt:55 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, fooLambdaParam:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int
// test.kt:56 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, fooLambdaParam:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, fooLamdbdaVar:int=2:int
// library.kt:12 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, fooLambdaParam:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, fooLamdbdaVar:int=2:int, barParam$iv:int=2:int, $i$f$bar:int=0:int
// library.kt:13 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, fooLambdaParam:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, fooLamdbdaVar:int=2:int, barParam$iv:int=2:int, $i$f$bar:int=0:int, barVar$iv:int=2:int
// test.kt:57 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, fooLambdaParam:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, fooLamdbdaVar:int=2:int, barParam$iv:int=2:int, $i$f$bar:int=0:int, barVar$iv:int=2:int, barLambdaParam:int=1:int, $i$a$-bar-TestKt$box$1$1:int=0:int
// test.kt:58 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, fooLambdaParam:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, fooLamdbdaVar:int=2:int, barParam$iv:int=2:int, $i$f$bar:int=0:int, barVar$iv:int=2:int, barLambdaParam:int=1:int, $i$a$-bar-TestKt$box$1$1:int=0:int, barLamdbdaVar:int=3:int
// library.kt:13 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, fooLambdaParam:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, fooLamdbdaVar:int=2:int, barParam$iv:int=2:int, $i$f$bar:int=0:int, barVar$iv:int=2:int
// library.kt:14 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, fooLambdaParam:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, fooLamdbdaVar:int=2:int, barParam$iv:int=2:int, $i$f$bar:int=0:int, barVar$iv:int=2:int
// test.kt:59 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int, fooLambdaParam:int=1:int, $i$a$-foo-TestKt$box$1:int=0:int, fooLamdbdaVar:int=2:int
// library.kt:8 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int
// library.kt:9 box: mainVar:int=1:int, fooParam$iv:int=1:int, $i$f$foo:int=0:int, fooVar$iv:int=1:int
// test.kt:60 box: mainVar:int=1:int
