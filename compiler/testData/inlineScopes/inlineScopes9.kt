// MODULE: library1
// ENABLE_INLINE_SCOPES_NUMBERS
// FILE: library1.kt

inline fun foo(fooParam: Int) {
    bar(1) {
        val inBlock1 = 1
        bar(2) {
            val inBlock2 = 2
        }
    }
}

inline fun bar(barParam: Int, block: (Int) -> Unit) {
    val barVal = 1
    block(1)
}

// MODULE: library2(library1)
// FILE: library2.kt

inline fun foo1() {
    foo(10)
}

// MODULE: test(library1, library2)
// ENABLE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    foo1()
    foo(10)
}

// EXPECTATIONS JVM_IR
// test.kt:31 box:
// library2.kt:23 box: $i$f$foo1\1\31:int=0:int
// library1.kt:6 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int
// library1.kt:15 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int
// library1.kt:16 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int
// library1.kt:7 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int, it\4$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\4\83\2$iv$iv:int=0:int
// library1.kt:8 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int, it\4$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\4\83\2$iv$iv:int=0:int, inBlock1\4$iv$iv:int=1:int
// library1.kt:15 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int, it\4$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\4\83\2$iv$iv:int=0:int, inBlock1\4$iv$iv:int=1:int, barParam\5$iv$iv$iv:int=2:int, $i$f$bar\5\75:int=0:int
// library1.kt:16 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int, it\4$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\4\83\2$iv$iv:int=0:int, inBlock1\4$iv$iv:int=1:int, barParam\5$iv$iv$iv:int=2:int, $i$f$bar\5\75:int=0:int, barVal\5$iv$iv$iv:int=1:int
// library1.kt:9 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int, it\4$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\4\83\2$iv$iv:int=0:int, inBlock1\4$iv$iv:int=1:int, barParam\5$iv$iv$iv:int=2:int, $i$f$bar\5\75:int=0:int, barVal\5$iv$iv$iv:int=1:int, it\6$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1$1\6\83\4$iv$iv:int=0:int
// library1.kt:10 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int, it\4$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\4\83\2$iv$iv:int=0:int, inBlock1\4$iv$iv:int=1:int, barParam\5$iv$iv$iv:int=2:int, $i$f$bar\5\75:int=0:int, barVal\5$iv$iv$iv:int=1:int, it\6$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1$1\6\83\4$iv$iv:int=0:int, inBlock2\6$iv$iv:int=2:int
// library1.kt:16 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int, it\4$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\4\83\2$iv$iv:int=0:int, inBlock1\4$iv$iv:int=1:int, barParam\5$iv$iv$iv:int=2:int, $i$f$bar\5\75:int=0:int, barVal\5$iv$iv$iv:int=1:int
// library1.kt:17 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int, it\4$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\4\83\2$iv$iv:int=0:int, inBlock1\4$iv$iv:int=1:int, barParam\5$iv$iv$iv:int=2:int, $i$f$bar\5\75:int=0:int, barVal\5$iv$iv$iv:int=1:int
// library1.kt:11 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int, it\4$iv$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\4\83\2$iv$iv:int=0:int, inBlock1\4$iv$iv:int=1:int
// library1.kt:16 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int
// library1.kt:17 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int, barParam\3$iv$iv$iv:int=1:int, $i$f$bar\3\73:int=0:int, barVal\3$iv$iv$iv:int=1:int
// library1.kt:12 box: $i$f$foo1\1\31:int=0:int, fooParam\2$iv$iv:int=10:int, $i$f$foo\2\72:int=0:int
// library2.kt:24 box: $i$f$foo1\1\31:int=0:int
// test.kt:32 box:
// library1.kt:6 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int
// library1.kt:15 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int
// library1.kt:16 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int
// library1.kt:7 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int, it\9$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\9\96\7$iv:int=0:int
// library1.kt:8 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int, it\9$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\9\96\7$iv:int=0:int, inBlock1\9$iv:int=1:int
// library1.kt:15 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int, it\9$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\9\96\7$iv:int=0:int, inBlock1\9$iv:int=1:int, barParam\10$iv$iv:int=2:int, $i$f$bar\10\88:int=0:int
// library1.kt:16 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int, it\9$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\9\96\7$iv:int=0:int, inBlock1\9$iv:int=1:int, barParam\10$iv$iv:int=2:int, $i$f$bar\10\88:int=0:int, barVal\10$iv$iv:int=1:int
// library1.kt:9 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int, it\9$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\9\96\7$iv:int=0:int, inBlock1\9$iv:int=1:int, barParam\10$iv$iv:int=2:int, $i$f$bar\10\88:int=0:int, barVal\10$iv$iv:int=1:int, it\11$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1$1\11\96\9$iv:int=0:int
// library1.kt:10 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int, it\9$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\9\96\7$iv:int=0:int, inBlock1\9$iv:int=1:int, barParam\10$iv$iv:int=2:int, $i$f$bar\10\88:int=0:int, barVal\10$iv$iv:int=1:int, it\11$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1$1\11\96\9$iv:int=0:int, inBlock2\11$iv:int=2:int
// library1.kt:16 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int, it\9$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\9\96\7$iv:int=0:int, inBlock1\9$iv:int=1:int, barParam\10$iv$iv:int=2:int, $i$f$bar\10\88:int=0:int, barVal\10$iv$iv:int=1:int
// library1.kt:17 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int, it\9$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\9\96\7$iv:int=0:int, inBlock1\9$iv:int=1:int, barParam\10$iv$iv:int=2:int, $i$f$bar\10\88:int=0:int, barVal\10$iv$iv:int=1:int
// library1.kt:11 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int, it\9$iv:int=1:int, $i$a$-bar-Library1Kt$foo$1\9\96\7$iv:int=0:int, inBlock1\9$iv:int=1:int
// library1.kt:16 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int
// library1.kt:17 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int, barParam\8$iv$iv:int=1:int, $i$f$bar\8\86:int=0:int, barVal\8$iv$iv:int=1:int
// library1.kt:12 box: fooParam\7$iv:int=10:int, $i$f$foo\7\32:int=0:int
// test.kt:33 box:
