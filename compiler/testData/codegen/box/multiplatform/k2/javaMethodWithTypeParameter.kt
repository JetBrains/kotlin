// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-59766
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// FULL_JDK

// MODULE: lib-jvm
// FILE: lib.kt

import java.util.concurrent.*

fun <T> CompletionStage<T>.foo() {
    handle { value, exception ->

    }
}

// MODULE: jvm()()(lib-jvm)
// FILE: jvm.kt

package kotlinx.coroutines.future

import java.util.concurrent.*
import java.util.function.*

fun <T> CompletionStage<T>.asDeferred() {
    handle { value, exception ->

    }
}

class ContinuationHandler<T> : BiFunction<T?, Throwable?, Unit> {
    override fun apply(result: T?, exception: Throwable?) {

    }
}

fun box(): String {
    val handler = ContinuationHandler<String>()
    CompletableFuture<String>().asDeferred()
    return "OK"
}
