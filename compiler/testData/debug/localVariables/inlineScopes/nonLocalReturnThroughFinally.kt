// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt
// KT-91572: the non-local return copies the `finally` blocks of `synchronized`, which splits the LVT ranges of the
// variables around it. All ranges of a variable keep the same scope number.

inline fun <T> sync(lock: Any, block: () -> T): T = synchronized(lock, block)

inline fun addIf(lock: Any, cond: (Int?) -> Boolean): Boolean = sync(lock) {
    val first: Int? = lock.hashCode()
    cond(first)
}

fun schedule(lock: Any, done: Boolean): Int = sync(lock) {
    addIf(lock) { firstTask ->
        if (done) return 1
        val t = firstTask ?: 0
        t > 0
    }
    0
}

fun box() {
    schedule(Any(), false)
}

// EXPECTATIONS JVM +USE_INLINE_SCOPES_NUMBERS
// test.kt:24 box:
// test.kt:14 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean
// test.kt:7 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int
// test.kt:15 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int
// test.kt:9 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int
// test.kt:7 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int, lock\4:java.lang.Object=java.lang.Object, $i$f$sync\4\71:int=0:int
// test.kt:10 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int, lock\4:java.lang.Object=java.lang.Object, $i$f$sync\4\71:int=0:int, $i$a$-sync-TestKt$addIf$1\5\72\3:int=0:int
// test.kt:11 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int, lock\4:java.lang.Object=java.lang.Object, $i$f$sync\4\71:int=0:int, $i$a$-sync-TestKt$addIf$1\5\72\3:int=0:int, first\5:int=1550089733:int
// test.kt:16 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int, lock\4:java.lang.Object=java.lang.Object, $i$f$sync\4\71:int=0:int, $i$a$-sync-TestKt$addIf$1\5\72\3:int=0:int, first\5:int=1550089733:int, firstTask\6:int=1550089733:int, $i$a$-addIf-TestKt$schedule$1$1\6\76\2:int=0:int
// test.kt:17 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int, lock\4:java.lang.Object=java.lang.Object, $i$f$sync\4\71:int=0:int, $i$a$-sync-TestKt$addIf$1\5\72\3:int=0:int, first\5:int=1550089733:int, firstTask\6:int=1550089733:int, $i$a$-addIf-TestKt$schedule$1$1\6\76\2:int=0:int
// test.kt:18 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int, lock\4:java.lang.Object=java.lang.Object, $i$f$sync\4\71:int=0:int, $i$a$-sync-TestKt$addIf$1\5\72\3:int=0:int, first\5:int=1550089733:int, firstTask\6:int=1550089733:int, $i$a$-addIf-TestKt$schedule$1$1\6\76\2:int=0:int, t\6:int=1550089733:int
// test.kt:11 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int, lock\4:java.lang.Object=java.lang.Object, $i$f$sync\4\71:int=0:int, $i$a$-sync-TestKt$addIf$1\5\72\3:int=0:int, first\5:int=1550089733:int
// test.kt:7 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int, lock\4:java.lang.Object=java.lang.Object, $i$f$sync\4\71:int=0:int
// test.kt:12 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int, lock\3:java.lang.Object=java.lang.Object, $i$f$addIf\3\15:int=0:int
// test.kt:20 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int, $i$a$-sync-TestKt$schedule$1\2\70\0:int=0:int
// test.kt:7 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock\1:java.lang.Object=java.lang.Object, $i$f$sync\1\14:int=0:int
// test.kt:21 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean
// test.kt:24 box:
// test.kt:25 box:

// EXPECTATIONS JVM
// test.kt:24 box:
// test.kt:14 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean
// test.kt:7 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int
// test.kt:15 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int, $i$a$-sync-TestKt$schedule$1:int=0:int
// test.kt:9 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$f$sync:int=0:int, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int
// test.kt:7 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int, lock$iv$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int
// test.kt:10 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int, lock$iv$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int, $i$a$-sync-TestKt$addIf$1$iv:int=0:int
// test.kt:11 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int, lock$iv$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int, $i$a$-sync-TestKt$addIf$1$iv:int=0:int, first$iv:int=1550089733:int
// test.kt:16 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int, lock$iv$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int, $i$a$-sync-TestKt$addIf$1$iv:int=0:int, first$iv:int=1550089733:int, firstTask:int=1550089733:int, $i$a$-addIf-TestKt$schedule$1$1:int=0:int
// test.kt:17 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int, lock$iv$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int, $i$a$-sync-TestKt$addIf$1$iv:int=0:int, first$iv:int=1550089733:int, firstTask:int=1550089733:int, $i$a$-addIf-TestKt$schedule$1$1:int=0:int
// test.kt:18 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int, lock$iv$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int, $i$a$-sync-TestKt$addIf$1$iv:int=0:int, first$iv:int=1550089733:int, firstTask:int=1550089733:int, $i$a$-addIf-TestKt$schedule$1$1:int=0:int, t:int=1550089733:int
// test.kt:11 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int, lock$iv$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int, $i$a$-sync-TestKt$addIf$1$iv:int=0:int, first$iv:int=1550089733:int
// test.kt:7 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int, lock$iv$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int
// test.kt:12 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, $i$f$sync:int=0:int, $i$a$-sync-TestKt$schedule$1:int=0:int, lock$iv:java.lang.Object=java.lang.Object, $i$f$addIf:int=0:int
// test.kt:20 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int, $i$a$-sync-TestKt$schedule$1:int=0:int
// test.kt:7 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean, lock$iv:java.lang.Object=java.lang.Object, $i$f$sync:int=0:int
// test.kt:21 schedule: lock:java.lang.Object=java.lang.Object, done:boolean=false:boolean
// test.kt:24 box:
// test.kt:25 box:
