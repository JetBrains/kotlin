// MODULE: library
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
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    bar() {
        val d = 4
    }
}

// EXPECTATIONS JVM_IR
// test.kt:52 box:
// library.kt:5 box: $i$f$bar\1\52:int=0:int
// library.kt:5 <init>:
// library.kt:21 box: $i$f$bar\1\52:int=0:int
// library.kt:7 baz: param:int=6:int
// library.kt:8 baz: param:int=6:int, b:int=2:int
// test.kt:53 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\1\8\0:int=0:int
// test.kt:54 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\1\8\0:int=0:int, d\1:int=4:int
// library.kt:8 baz: param:int=6:int, b:int=2:int
// library.kt:9 baz: param:int=6:int, b:int=2:int
// library.kt:25 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int
// library.kt:38 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int
// library.kt:44 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz2Param\4:int=1:int, $i$f$baz2\4\53:int=0:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz2Param\4:int=1:int, $i$f$baz2\4\53:int=0:int, baz2Var\4:int=3:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int
// library.kt:27 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int
// library.kt:28 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int
// library.kt:38 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int, baz1Param\6:int=2:int, $i$f$baz1\6\61:int=0:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int, baz1Param\6:int=2:int, $i$f$baz1\6\61:int=0:int, baz1Var\6:int=3:int
// library.kt:44 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int, baz1Param\6:int=2:int, $i$f$baz1\6\61:int=0:int, baz1Var\6:int=3:int, baz2Param\7:int=1:int, $i$f$baz2\7\72:int=0:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int, baz1Param\6:int=2:int, $i$f$baz1\6\61:int=0:int, baz1Var\6:int=3:int, baz2Param\7:int=1:int, $i$f$baz2\7\72:int=0:int, baz2Var\7:int=3:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int, baz1Param\6:int=2:int, $i$f$baz1\6\61:int=0:int, baz1Var\6:int=3:int
// library.kt:29 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int, baz1Param\6:int=2:int, $i$f$baz1\6\61:int=0:int, baz1Var\6:int=3:int, baz1BlockParam1\8:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\8\73\5:int=0:int
// library.kt:30 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int, baz1Param\6:int=2:int, $i$f$baz1\6\61:int=0:int, baz1Var\6:int=3:int, baz1BlockParam1\8:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\8\73\5:int=0:int, baz1LambdaVar1\8:int=1:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int, baz1Param\6:int=2:int, $i$f$baz1\6\61:int=0:int, baz1Var\6:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int, baz1Param\6:int=2:int, $i$f$baz1\6\61:int=0:int, baz1Var\6:int=3:int
// library.kt:32 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int, baz1BlockParam\5:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\5\54\2:int=0:int, baz1LambdaVar\5:int=1:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, baz1Param\3:int=1:int, $i$f$baz1\3\51:int=0:int, baz1Var\3:int=3:int
// library.kt:34 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int
// library.kt:10 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$1\9\67\0:int=0:int
// library.kt:11 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$1\9\67\0:int=0:int, g\9:int=7:int
// library.kt:34 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int
// library.kt:35 baz: param:int=6:int, b:int=2:int, inlineCallParam1\2:int=1:int, inlineCallParam2\2:int=2:int, $i$f$inlineCall\2\9:int=0:int, e\2:int=5:int
// library.kt:12 baz: param:int=6:int, b:int=2:int
// test.kt:53 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\10\12\0:int=0:int
// test.kt:54 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\10\12\0:int=0:int, d\10:int=4:int
// library.kt:12 baz: param:int=6:int, b:int=2:int
// library.kt:13 baz: param:int=6:int, b:int=2:int
// library.kt:25 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int
// library.kt:38 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int
// library.kt:44 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz2Param\13:int=1:int, $i$f$baz2\13\82:int=0:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz2Param\13:int=1:int, $i$f$baz2\13\82:int=0:int, baz2Var\13:int=3:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int
// library.kt:27 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int
// library.kt:28 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int
// library.kt:38 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int, baz1Param\15:int=2:int, $i$f$baz1\15\90:int=0:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int, baz1Param\15:int=2:int, $i$f$baz1\15\90:int=0:int, baz1Var\15:int=3:int
// library.kt:44 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int, baz1Param\15:int=2:int, $i$f$baz1\15\90:int=0:int, baz1Var\15:int=3:int, baz2Param\16:int=1:int, $i$f$baz2\16\101:int=0:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int, baz1Param\15:int=2:int, $i$f$baz1\15\90:int=0:int, baz1Var\15:int=3:int, baz2Param\16:int=1:int, $i$f$baz2\16\101:int=0:int, baz2Var\16:int=3:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int, baz1Param\15:int=2:int, $i$f$baz1\15\90:int=0:int, baz1Var\15:int=3:int
// library.kt:29 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int, baz1Param\15:int=2:int, $i$f$baz1\15\90:int=0:int, baz1Var\15:int=3:int, baz1BlockParam1\17:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\17\102\14:int=0:int
// library.kt:30 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int, baz1Param\15:int=2:int, $i$f$baz1\15\90:int=0:int, baz1Var\15:int=3:int, baz1BlockParam1\17:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\17\102\14:int=0:int, baz1LambdaVar1\17:int=1:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int, baz1Param\15:int=2:int, $i$f$baz1\15\90:int=0:int, baz1Var\15:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int, baz1Param\15:int=2:int, $i$f$baz1\15\90:int=0:int, baz1Var\15:int=3:int
// library.kt:32 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int, baz1BlockParam\14:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\14\83\11:int=0:int, baz1LambdaVar\14:int=1:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, baz1Param\12:int=1:int, $i$f$baz1\12\80:int=0:int, baz1Var\12:int=3:int
// library.kt:34 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int
// library.kt:14 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$2\18\96\0:int=0:int
// library.kt:15 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$2\18\96\0:int=0:int, g\18:int=7:int
// library.kt:34 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int
// library.kt:35 baz: param:int=6:int, b:int=2:int, inlineCallParam1\11:int=1:int, inlineCallParam2\11:int=2:int, $i$f$inlineCall\11\13:int=0:int, e\11:int=5:int
// library.kt:16 baz: param:int=6:int, b:int=2:int
// test.kt:53 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\19\16\0:int=0:int
// test.kt:54 baz: param:int=6:int, b:int=2:int, $i$a$-bar-TestKt$box$1\19\16\0:int=0:int, d\19:int=4:int
// library.kt:16 baz: param:int=6:int, b:int=2:int
// library.kt:17 baz: param:int=6:int, b:int=2:int
// library.kt:25 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int
// library.kt:26 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int
// library.kt:38 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int
// library.kt:44 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz2Param\22:int=1:int, $i$f$baz2\22\111:int=0:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz2Param\22:int=1:int, $i$f$baz2\22\111:int=0:int, baz2Var\22:int=3:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int
// library.kt:27 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int
// library.kt:28 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int
// library.kt:38 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int, baz1Param\24:int=2:int, $i$f$baz1\24\119:int=0:int
// library.kt:39 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int, baz1Param\24:int=2:int, $i$f$baz1\24\119:int=0:int, baz1Var\24:int=3:int
// library.kt:44 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int, baz1Param\24:int=2:int, $i$f$baz1\24\119:int=0:int, baz1Var\24:int=3:int, baz2Param\25:int=1:int, $i$f$baz2\25\130:int=0:int
// library.kt:45 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int, baz1Param\24:int=2:int, $i$f$baz1\24\119:int=0:int, baz1Var\24:int=3:int, baz2Param\25:int=1:int, $i$f$baz2\25\130:int=0:int, baz2Var\25:int=3:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int, baz1Param\24:int=2:int, $i$f$baz1\24\119:int=0:int, baz1Var\24:int=3:int
// library.kt:29 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int, baz1Param\24:int=2:int, $i$f$baz1\24\119:int=0:int, baz1Var\24:int=3:int, baz1BlockParam1\26:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\26\131\23:int=0:int
// library.kt:30 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int, baz1Param\24:int=2:int, $i$f$baz1\24\119:int=0:int, baz1Var\24:int=3:int, baz1BlockParam1\26:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1$1\26\131\23:int=0:int, baz1LambdaVar1\26:int=1:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int, baz1Param\24:int=2:int, $i$f$baz1\24\119:int=0:int, baz1Var\24:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int, baz1Param\24:int=2:int, $i$f$baz1\24\119:int=0:int, baz1Var\24:int=3:int
// library.kt:32 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int, baz1BlockParam\23:int=1:int, $i$a$-baz1-LibraryKt$inlineCall$1\23\112\20:int=0:int, baz1LambdaVar\23:int=1:int
// library.kt:40 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int
// library.kt:41 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, baz1Param\21:int=1:int, $i$f$baz1\21\109:int=0:int, baz1Var\21:int=3:int
// library.kt:34 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int
// library.kt:18 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$3\27\125\0:int=0:int
// library.kt:19 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int, $i$a$-inlineCall-LibraryKt$bar$1$baz$3\27\125\0:int=0:int, g\27:int=7:int
// library.kt:34 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int
// library.kt:35 baz: param:int=6:int, b:int=2:int, inlineCallParam1\20:int=1:int, inlineCallParam2\20:int=2:int, $i$f$inlineCall\20\17:int=0:int, e\20:int=5:int
// library.kt:20 baz: param:int=6:int, b:int=2:int
// library.kt:22 box: $i$f$bar\1\52:int=0:int
// test.kt:55 box:
