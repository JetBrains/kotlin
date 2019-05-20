package foo

internal expect abstract class CompletionHandlerBase() {
    abstract fun invoke(cause: Throwable?)
}

internal abstract class JobNode : CompletionHandlerBase()

public typealias CompletionHandler = (cause: Throwable?) -> Unit

fun bar(x: CompletionHandler) { x.hashCode() }
