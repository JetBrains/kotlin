// MODULE: library
// FILE: library.kt

inline fun <R> analyze(
    action: () -> R,
): R {
    "Start"
    return try {
        action()
    } finally {
        "End"
    }
}


inline fun foo(a: Int, b: Int) {
    analyze {
        foo() ?: return
        a + b
        42 // Breakpoint here
    }
}

inline fun bar() {
    foo(1, 2)
}

fun foo(): Int? = 42

// MODULE: test(library)
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    bar()
}

// EXPECTATIONS JVM_IR
// test.kt:35 box:
// library.kt:25 box: $i$f$bar\1\35:int=0:int
// library.kt:17 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int
// library.kt:7 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int
// library.kt:8 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int
// library.kt:9 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int
// library.kt:18 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int, $i$a$-analyze-LibraryKt$foo$1\4\82\2:int=0:int
// library.kt:28 foo:
// library.kt:18 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int, $i$a$-analyze-LibraryKt$foo$1\4\82\2:int=0:int
// library.kt:11 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\84:int=0:int, $i$a$-analyze-LibraryKt$foo$1\6\84\2:int=0:int
// library.kt:19 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\84:int=0:int, $i$a$-analyze-LibraryKt$foo$1\6\84\2:int=0:int
// library.kt:20 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\84:int=0:int, $i$a$-analyze-LibraryKt$foo$1\6\84\2:int=0:int
// library.kt:9 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\84:int=0:int
// library.kt:12 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\84:int=0:int
// library.kt:8 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\84:int=0:int
// library.kt:22 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int
// library.kt:26 box: $i$f$bar\1\35:int=0:int
// test.kt:36 box:

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:35 box:
// library.kt:25 box: $i$f$bar\1\35:int=0:int
// library.kt:17 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int
// library.kt:7 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int
// library.kt:8 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int
// library.kt:9 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int
// library.kt:18 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int, $i$a$-analyze-LibraryKt$foo$1\4\82\2:int=0:int
// library.kt:28 foo:
// library.kt:18 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\3\79:int=0:int, $i$a$-analyze-LibraryKt$foo$1\4\82\2:int=0:int
// library.kt:11 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\79:int=0:int, $i$a$-analyze-LibraryKt$foo$1\6\82\2:int=0:int
// library.kt:19 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\79:int=0:int, $i$a$-analyze-LibraryKt$foo$1\6\82\2:int=0:int
// library.kt:20 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\79:int=0:int, $i$a$-analyze-LibraryKt$foo$1\6\82\2:int=0:int
// library.kt:9 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\79:int=0:int
// library.kt:12 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\79:int=0:int
// library.kt:8 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int, $i$f$analyze\5\79:int=0:int
// library.kt:22 box: $i$f$bar\1\35:int=0:int, a\2:int=1:int, b\2:int=2:int, $i$f$foo\2\78:int=0:int
// library.kt:26 box: $i$f$bar\1\35:int=0:int
// test.kt:36 box:
