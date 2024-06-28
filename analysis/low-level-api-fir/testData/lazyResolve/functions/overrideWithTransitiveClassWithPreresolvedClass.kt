// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// WITH_STDLIB
// MODULE: top-common
// FILE: top.kt
typealias CompletionHandler = (cause: Throwable?) -> Unit

abstract class JobNode() : CompletionHandlerBase() {
    val a<caret_preresolved> = 4
}

expect abstract class CompletionHandlerBase() {
    abstract fun invoke(cause: Throwable?)
}

// MODULE: middle-common()()(top-common)
// FILE: middle.kt
actual abstract class CompletionHandlerBase actual constructor() : CompletionHandler {
    actual abstract override fun invoke(cause: Throwable?)
}

// MODULE: main-jvm()()(middle-common)
// FILE: bottom.kt

class CancelFutureOnCompletion() : JobNode() {
    override fun inv<caret>oke(cause: Throwable?) {
    }
}
