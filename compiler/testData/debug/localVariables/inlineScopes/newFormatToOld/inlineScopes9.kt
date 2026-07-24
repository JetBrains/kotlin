// MODULE: library
// USE_INLINE_SCOPES_NUMBERS
// FILE: library.kt

inline fun bar(crossinline block: () -> Unit) {
    object {
        fun baz(param: Int) {
            val b = 2
            block()
            inlineCall(1, 2) {
                val g = 7
            }
            block()
            inlineCall(1, 2) {
                val g = 7
            }
            block()
            inlineCall(1, 2) {
                val g = 7
            }
        }
    }.baz(6)
}

inline fun inlineCall(inlineCallParam1: Int, inlineCallParam2: Int, block: () -> Unit) {
    val e = 5
    baz1(1) { baz1BlockParam ->
        val baz1LambdaVar = 1
        baz1(2) { baz1BlockParam1 ->
            val baz1LambdaVar1 = 1
            3
        }
        2
    }
    block()
}

inline fun baz1(baz1Param: Int, block: (Int) -> Int) {
    val baz1Var = 3
    baz2(1)
    block(1)
}

inline fun baz2(baz2Param: Int) {
    val baz2Var = 3
}

// MODULE: test(library)
// FILE: test.kt

fun box() {
    bar() {
        val d = 4
    }
}

// EXPECTATIONS JVM_IR
// test.kt:52 box:
// library.kt:6 box: $i$f$bar:int=0:int
// library.kt:6 <init>:
// library.kt:22 box: $i$f$bar:int=0:int
// library.kt:8 baz: param:int=6:int
// library.kt:9 baz: param:int=6:int, b:int=2:int
// test.kt:53 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:54 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int, d:int=4:int
// library.kt:9 baz: param:int=6:int, b:int=2:int
// library.kt:10 baz: param:int=6:int, b:int=2:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int
// library.kt:27 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz2Param\3:int=1:int, $i$f$baz2\3\52:int=0:int
// library.kt:46 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz2Param\3:int=1:int, $i$f$baz2\3\52:int=0:int, baz2Var\3:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int
// library.kt:28 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int
// library.kt:29 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int, baz1Param\5:int=2:int, $i$f$baz1\5\60:int=0:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int, baz1Param\5:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5:int=3:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int, baz1Param\5:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5:int=3:int, baz2Param\6:int=1:int, $i$f$baz2\6\71:int=0:int
// library.kt:46 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int, baz1Param\5:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5:int=3:int, baz2Param\6:int=1:int, $i$f$baz2\6\71:int=0:int, baz2Var\6:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int, baz1Param\5:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5:int=3:int
// library.kt:30 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int, baz1Param\5:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5:int=3:int, baz1BlockParam1\7:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\7\72\4:int=0:int
// library.kt:31 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int, baz1Param\5:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5:int=3:int, baz1BlockParam1\7:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\7\72\4:int=0:int, baz1LambdaVar1\7:int=1:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int, baz1Param\5:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5:int=3:int
// library.kt:42 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int, baz1Param\5:int=2:int, $i$f$baz1\5\60:int=0:int, baz1Var\5:int=3:int
// library.kt:33 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int, baz1BlockParam\4:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\4\53\1:int=0:int, baz1LambdaVar\4:int=1:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int
// library.kt:42 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, baz1Param\2:int=1:int, $i$f$baz1\2\50:int=0:int, baz1Var\2:int=3:int
// library.kt:35 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int
// library.kt:11 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$1\8\66\0:int=0:int
// library.kt:12 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$1\8\66\0:int=0:int, g\8:int=7:int
// library.kt:35 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int
// library.kt:36 baz: param:int=6:int, b:int=2:int, inlineCallParam1\1:int=1:int, inlineCallParam2\1:int=2:int, $i$f$inlineCall\1\10:int=0:int, e\1:int=5:int
// library.kt:13 baz: param:int=6:int, b:int=2:int
// test.kt:53 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:54 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int, d:int=4:int
// library.kt:13 baz: param:int=6:int, b:int=2:int
// library.kt:14 baz: param:int=6:int, b:int=2:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int
// library.kt:27 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz2Param\11:int=1:int, $i$f$baz2\11\81:int=0:int
// library.kt:46 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz2Param\11:int=1:int, $i$f$baz2\11\81:int=0:int, baz2Var\11:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int
// library.kt:28 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int
// library.kt:29 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int, baz1Param\13:int=2:int, $i$f$baz1\13\89:int=0:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int, baz1Param\13:int=2:int, $i$f$baz1\13\89:int=0:int, baz1Var\13:int=3:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int, baz1Param\13:int=2:int, $i$f$baz1\13\89:int=0:int, baz1Var\13:int=3:int, baz2Param\14:int=1:int, $i$f$baz2\14\100:int=0:int
// library.kt:46 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int, baz1Param\13:int=2:int, $i$f$baz1\13\89:int=0:int, baz1Var\13:int=3:int, baz2Param\14:int=1:int, $i$f$baz2\14\100:int=0:int, baz2Var\14:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int, baz1Param\13:int=2:int, $i$f$baz1\13\89:int=0:int, baz1Var\13:int=3:int
// library.kt:30 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int, baz1Param\13:int=2:int, $i$f$baz1\13\89:int=0:int, baz1Var\13:int=3:int, baz1BlockParam1\15:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\15\101\12:int=0:int
// library.kt:31 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int, baz1Param\13:int=2:int, $i$f$baz1\13\89:int=0:int, baz1Var\13:int=3:int, baz1BlockParam1\15:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\15\101\12:int=0:int, baz1LambdaVar1\15:int=1:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int, baz1Param\13:int=2:int, $i$f$baz1\13\89:int=0:int, baz1Var\13:int=3:int
// library.kt:42 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int, baz1Param\13:int=2:int, $i$f$baz1\13\89:int=0:int, baz1Var\13:int=3:int
// library.kt:33 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int, baz1BlockParam\12:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\12\82\9:int=0:int, baz1LambdaVar\12:int=1:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int
// library.kt:42 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, baz1Param\10:int=1:int, $i$f$baz1\10\79:int=0:int, baz1Var\10:int=3:int
// library.kt:35 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int
// library.kt:15 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$2\16\95\0:int=0:int
// library.kt:16 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$2\16\95\0:int=0:int, g\16:int=7:int
// library.kt:35 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int
// library.kt:36 baz: param:int=6:int, b:int=2:int, inlineCallParam1\9:int=1:int, inlineCallParam2\9:int=2:int, $i$f$inlineCall\9\14:int=0:int, e\9:int=5:int
// library.kt:17 baz: param:int=6:int, b:int=2:int
// test.kt:53 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int
// test.kt:54 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1:int=0:int, d:int=4:int
// library.kt:17 baz: param:int=6:int, b:int=2:int
// library.kt:18 baz: param:int=6:int, b:int=2:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int
// library.kt:27 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz2Param\19:int=1:int, $i$f$baz2\19\110:int=0:int
// library.kt:46 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz2Param\19:int=1:int, $i$f$baz2\19\110:int=0:int, baz2Var\19:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int
// library.kt:28 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int
// library.kt:29 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int, baz1Param\21:int=2:int, $i$f$baz1\21\118:int=0:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int, baz1Param\21:int=2:int, $i$f$baz1\21\118:int=0:int, baz1Var\21:int=3:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int, baz1Param\21:int=2:int, $i$f$baz1\21\118:int=0:int, baz1Var\21:int=3:int, baz2Param\22:int=1:int, $i$f$baz2\22\129:int=0:int
// library.kt:46 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int, baz1Param\21:int=2:int, $i$f$baz1\21\118:int=0:int, baz1Var\21:int=3:int, baz2Param\22:int=1:int, $i$f$baz2\22\129:int=0:int, baz2Var\22:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int, baz1Param\21:int=2:int, $i$f$baz1\21\118:int=0:int, baz1Var\21:int=3:int
// library.kt:30 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int, baz1Param\21:int=2:int, $i$f$baz1\21\118:int=0:int, baz1Var\21:int=3:int, baz1BlockParam1\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\23\130\20:int=0:int
// library.kt:31 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int, baz1Param\21:int=2:int, $i$f$baz1\21\118:int=0:int, baz1Var\21:int=3:int, baz1BlockParam1\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\23\130\20:int=0:int, baz1LambdaVar1\23:int=1:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int, baz1Param\21:int=2:int, $i$f$baz1\21\118:int=0:int, baz1Var\21:int=3:int
// library.kt:42 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int, baz1Param\21:int=2:int, $i$f$baz1\21\118:int=0:int, baz1Var\21:int=3:int
// library.kt:33 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int, baz1BlockParam\20:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\20\111\17:int=0:int, baz1LambdaVar\20:int=1:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int
// library.kt:42 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, baz1Param\18:int=1:int, $i$f$baz1\18\108:int=0:int, baz1Var\18:int=3:int
// library.kt:35 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int
// library.kt:19 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$3\24\124\0:int=0:int
// library.kt:20 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$3\24\124\0:int=0:int, g\24:int=7:int
// library.kt:35 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int
// library.kt:36 baz: param:int=6:int, b:int=2:int, inlineCallParam1\17:int=1:int, inlineCallParam2\17:int=2:int, $i$f$inlineCall\17\18:int=0:int, e\17:int=5:int
// library.kt:21 baz: param:int=6:int, b:int=2:int
// library.kt:23 box: $i$f$bar:int=0:int
// test.kt:55 box:

// EXPECTATIONS WASM
// test.kt:52 $box: (4)
// library.kt:6 $box: (4)
// library.kt:22 $<no name provided>.<init>: $<this>:(ref $<no name provided>)=(ref $<no name provided>) (5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
// library.kt:22 $box: (10, 6)
// library.kt:8 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=0:i32, $d:i32=0:i32, $e:i32=0:i32, $baz1Var:i32=0:i32, $baz2Var:i32=0:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (20, 20)
// library.kt:9 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=0:i32, $e:i32=0:i32, $baz1Var:i32=0:i32, $baz2Var:i32=0:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (12)
// test.kt:53 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=0:i32, $baz1Var:i32=0:i32, $baz2Var:i32=0:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (16, 16, 16)
// test.kt:54 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=0:i32, $baz1Var:i32=0:i32, $baz2Var:i32=0:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (5, 5)
// library.kt:10 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=0:i32, $baz1Var:i32=0:i32, $baz2Var:i32=0:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (23, 23, 26, 26, 12, 12, 12, 12, 12)
// library.kt:26 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=0:i32, $baz1Var:i32=0:i32, $baz2Var:i32=0:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (12, 12)
// library.kt:27 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=0:i32, $baz2Var:i32=0:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (9, 9, 4, 4, 4)
// library.kt:39 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=0:i32, $baz2Var:i32=0:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (18, 18)
// library.kt:40 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=0:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (9, 9, 4, 4, 4)
// library.kt:45 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (18, 18, 18)
// library.kt:46 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (1, 1)
// library.kt:41 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (10, 10, 4, 4, 4)
// library.kt:28 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=0:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (28, 28)
// library.kt:29 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (13, 13, 8, 8, 8)
// library.kt:39 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (18, 18)
// library.kt:40 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (9, 9, 4, 4, 4)
// library.kt:45 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (18, 18, 18)
// library.kt:46 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (1, 1)
// library.kt:41 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (10, 10, 4, 4, 4)
// library.kt:30 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=0:i32, $g:i32=0:i32 (33, 33)
// library.kt:31 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=0:i32 (12, 12, 12, 12, 12, 12, 13, 13)
// library.kt:42 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=0:i32 (1, 1)
// library.kt:33 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=0:i32 (8, 8, 8, 8, 8, 8, 9, 9)
// library.kt:42 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=0:i32 (1, 1)
// library.kt:35 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=0:i32 (4)
// library.kt:11 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (24, 24, 24)
// library.kt:12 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (13, 13, 13)
// library.kt:36 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:13 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (12)
// test.kt:53 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (16, 16, 16)
// test.kt:54 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (5, 5)
// library.kt:14 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (23, 23, 26, 26, 12, 12, 12, 12, 12)
// library.kt:26 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (12, 12)
// library.kt:27 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (9, 9, 4, 4, 4)
// library.kt:39 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (18, 18)
// library.kt:40 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (9, 9, 4, 4, 4)
// library.kt:45 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (18, 18, 18)
// library.kt:46 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:41 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (10, 10, 4, 4, 4)
// library.kt:28 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (28, 28)
// library.kt:29 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (13, 13, 8, 8, 8)
// library.kt:39 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (18, 18)
// library.kt:40 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (9, 9, 4, 4, 4)
// library.kt:45 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (18, 18, 18)
// library.kt:46 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:41 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (10, 10, 4, 4, 4)
// library.kt:30 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (33, 33)
// library.kt:31 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (12, 12, 12, 12, 12, 12, 13, 13)
// library.kt:42 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:33 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (8, 8, 8, 8, 8, 8, 9, 9)
// library.kt:42 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:35 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (4)
// library.kt:15 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (24, 24, 24)
// library.kt:16 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (13, 13, 13)
// library.kt:36 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:17 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (12)
// test.kt:53 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (16, 16, 16)
// test.kt:54 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (5, 5)
// library.kt:18 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (23, 23, 26, 26, 12, 12, 12, 12, 12)
// library.kt:26 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (12, 12)
// library.kt:27 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (9, 9, 4, 4, 4)
// library.kt:39 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (18, 18)
// library.kt:40 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (9, 9, 4, 4, 4)
// library.kt:45 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (18, 18, 18)
// library.kt:46 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:41 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (10, 10, 4, 4, 4)
// library.kt:28 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (28, 28)
// library.kt:29 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (13, 13, 8, 8, 8)
// library.kt:39 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (18, 18)
// library.kt:40 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (9, 9, 4, 4, 4)
// library.kt:45 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (18, 18, 18)
// library.kt:46 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:41 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (10, 10, 4, 4, 4)
// library.kt:30 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (33, 33)
// library.kt:31 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (12, 12, 12, 12, 12, 12, 13, 13)
// library.kt:42 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:33 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (8, 8, 8, 8, 8, 8, 9, 9)
// library.kt:42 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:35 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (4)
// library.kt:19 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (24, 24, 24)
// library.kt:20 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (13, 13, 13)
// library.kt:36 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (1, 1)
// library.kt:21 $<no name provided>.baz: $<this>:(ref $<no name provided>)=(ref $<no name provided>), $param:i32=6:i32, $b:i32=2:i32, $d:i32=4:i32, $e:i32=5:i32, $baz1Var:i32=3:i32, $baz2Var:i32=3:i32, $baz1LambdaVar:i32=1:i32, $baz1LambdaVar1:i32=1:i32, $g:i32=7:i32 (9, 9)
// library.kt:22 $box: (6)
// library.kt:23 $box: (1)
// test.kt:55 $box: (1)
