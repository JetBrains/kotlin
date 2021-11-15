// WITH_STDLIB
import kotlin.coroutines.*

fun <T, R> T.map(transform: suspend (T) -> R): suspend () -> R =
    { transform(this) }

fun runs(f: suspend () -> String?): String {
    var result: String? = ""
    f.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result ?: "Fail"
}

fun box(): String {
    return runs {
        Result.success("OK").map<Result<String>, String?> { it.getOrNull() }()
    }
}
