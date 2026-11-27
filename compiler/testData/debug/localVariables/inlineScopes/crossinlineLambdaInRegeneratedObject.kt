// TARGET_BACKEND: JVM
// FILE: test.kt
// KT-91578: in the regenerated object, the crossinline lambda gets a scope number between the scopes that start before
// and after its call. The variables of `tryCatch` that start after the call keep the scope of `tryCatch`.

interface Source {
    fun subscribe(observer: (Int) -> Unit)
}

inline fun tryCatch(
    errorTransformer: (Throwable) -> Throwable = { it },
    block: () -> Unit
) {
    try {
        block()
    } catch (e: Throwable) {
        val transformedError = errorTransformer(e)
        transformedError.hashCode()
    }
}

inline fun source(crossinline onSubscribe: ((Int) -> Unit) -> Unit): Source =
    object : Source {
        override fun subscribe(observer: (Int) -> Unit) {
            tryCatch {
                onSubscribe(observer)
            }
        }
    }

fun box() {
    source { emit -> emit(1); throw IllegalStateException("fail") }.subscribe { }
}

// EXPECTATIONS JVM +USE_INLINE_SCOPES_NUMBERS
// test.kt:32 box:
// test.kt:23 box: $i$f$source\1\32:int=0:int
// test.kt:23 <init>:
// test.kt:23 box: $i$f$source\1\32:int=0:int
// test.kt:29 box: $i$f$source\1\32:int=0:int
// test.kt:32 box:
// test.kt:25 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:10 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:14 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int
// test.kt:15 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int
// test.kt:26 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int, $i$a$-tryCatch$default-TestKt$source$1$subscribe$1\2\88\0:int=0:int
// test.kt:32 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int, $i$a$-tryCatch$default-TestKt$source$1$subscribe$1\2\88\0:int=0:int, emit\3:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$a$-source-TestKt$box$1\3\26\0:int=0:int
// test.kt:32 box$lambda$1: it:int=1:int
// test.kt:32 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int, $i$a$-tryCatch$default-TestKt$source$1$subscribe$1\2\88\0:int=0:int, emit\3:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$a$-source-TestKt$box$1\3\26\0:int=0:int
// test.kt:16 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int
// test.kt:17 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int, e\1:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:11 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int, e\1:java.lang.Throwable=java.lang.IllegalStateException, $i$a$-tryCatch-TestKt$tryCatch$1\4\91\0:int=0:int, it\4:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:17 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int, e\1:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:18 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int, e\1:java.lang.Throwable=java.lang.IllegalStateException, transformedError\1:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:20 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch\1\25:int=0:int
// test.kt:28 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:33 box:

// EXPECTATIONS JVM
// test.kt:32 box:
// test.kt:23 box: $i$f$source:int=0:int
// test.kt:23 <init>:
// test.kt:23 box: $i$f$source:int=0:int
// test.kt:29 box: $i$f$source:int=0:int
// test.kt:32 box:
// test.kt:25 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:10 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:14 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int
// test.kt:15 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int
// test.kt:26 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int, $i$a$-tryCatch$default-TestKt$source$1$subscribe$1:int=0:int
// test.kt:32 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int, $i$a$-tryCatch$default-TestKt$source$1$subscribe$1:int=0:int, emit:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$a$-source-TestKt$box$1:int=0:int
// test.kt:32 box$lambda$1: it:int=1:int
// test.kt:32 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int, $i$a$-tryCatch$default-TestKt$source$1$subscribe$1:int=0:int, emit:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$a$-source-TestKt$box$1:int=0:int
// test.kt:16 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int
// test.kt:17 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int, e$iv:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:11 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int, e$iv:java.lang.Throwable=java.lang.IllegalStateException, $i$a$-tryCatch-TestKt$tryCatch$1:int=0:int, it:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:17 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int, e$iv:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:18 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int, e$iv:java.lang.Throwable=java.lang.IllegalStateException, transformedError$iv:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:20 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>, $i$f$tryCatch:int=0:int
// test.kt:28 subscribe: observer:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:33 box:
