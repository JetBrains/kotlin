// WITH_STDLIB
// MODULE: library
// FILE: library.kt
// KT-91572: the non-local return in `firstPositive` copies the `finally` block of `synchronized`, which splits the
// LVT ranges of the variables around it in the old format. All ranges of a variable get the same scope number.

inline fun <T> sync(lock: Any, block: () -> T): T = synchronized(lock, block)

inline fun <T> List<T>.forEach2(action: (T) -> Unit) {
    for (element in this) action(element)
}

inline fun firstPositive(lock: Any, list: List<Int>): Int = sync(lock) {
    val size = list.size
    list.forEach2 { x ->
        if (x > 0) return x
    }
    -size
}

// MODULE: test(library)
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt

fun box() {
    firstPositive(Any(), listOf(0, 1))
}

// EXPECTATIONS JVM
// test.kt:26 box:
// library.kt:13 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int
// library.kt:7 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int, lock\2:java.lang.Object=java.lang.Object, $i$f$sync\2\44:int=0:int
// library.kt:14 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int, lock\2:java.lang.Object=java.lang.Object, $i$f$sync\2\44:int=0:int, $i$a$-sync-LibraryKt$firstPositive$1\3\45\1:int=0:int
// library.kt:15 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int, lock\2:java.lang.Object=java.lang.Object, $i$f$sync\2\44:int=0:int, $i$a$-sync-LibraryKt$firstPositive$1\3\45\1:int=0:int, size\3:int=2:int
// library.kt:10 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int, lock\2:java.lang.Object=java.lang.Object, $i$f$sync\2\44:int=0:int, $i$a$-sync-LibraryKt$firstPositive$1\3\45\1:int=0:int, size\3:int=2:int, $this$forEach2\4:java.util.List=java.util.Arrays$ArrayList, $i$f$forEach2\4\53:int=0:int
// library.kt:16 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int, lock\2:java.lang.Object=java.lang.Object, $i$f$sync\2\44:int=0:int, $i$a$-sync-LibraryKt$firstPositive$1\3\45\1:int=0:int, size\3:int=2:int, $this$forEach2\4:java.util.List=java.util.Arrays$ArrayList, $i$f$forEach2\4\53:int=0:int, element\4:java.lang.Object=java.lang.Integer, x\5:int=0:int, $i$a$-forEach2-LibraryKt$firstPositive$1$1\5\48\3:int=0:int
// library.kt:17 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int, lock\2:java.lang.Object=java.lang.Object, $i$f$sync\2\44:int=0:int, $i$a$-sync-LibraryKt$firstPositive$1\3\45\1:int=0:int, size\3:int=2:int, $this$forEach2\4:java.util.List=java.util.Arrays$ArrayList, $i$f$forEach2\4\53:int=0:int, element\4:java.lang.Object=java.lang.Integer, x\5:int=0:int, $i$a$-forEach2-LibraryKt$firstPositive$1$1\5\48\3:int=0:int
// library.kt:10 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int, lock\2:java.lang.Object=java.lang.Object, $i$f$sync\2\44:int=0:int, $i$a$-sync-LibraryKt$firstPositive$1\3\45\1:int=0:int, size\3:int=2:int, $this$forEach2\4:java.util.List=java.util.Arrays$ArrayList, $i$f$forEach2\4\53:int=0:int, element\4:java.lang.Object=java.lang.Integer
// library.kt:16 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int, lock\2:java.lang.Object=java.lang.Object, $i$f$sync\2\44:int=0:int, $i$a$-sync-LibraryKt$firstPositive$1\3\45\1:int=0:int, size\3:int=2:int, $this$forEach2\4:java.util.List=java.util.Arrays$ArrayList, $i$f$forEach2\4\53:int=0:int, element\4:java.lang.Object=java.lang.Integer, x\5:int=1:int, $i$a$-forEach2-LibraryKt$firstPositive$1$1\5\48\3:int=0:int
// library.kt:7 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int
// library.kt:16 box: lock\1:java.lang.Object=java.lang.Object, list\1:java.util.List=java.util.Arrays$ArrayList, $i$f$firstPositive\1\26:int=0:int
// test.kt:27 box:
