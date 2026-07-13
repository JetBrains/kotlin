import java.util.concurrent.Executors
import java.util.concurrent.Future

fun expectFutureString(f: Future<String>) {}

fun main() {
    val executor = Executors.newSingleThreadExecutor()
    // With -Xeager-lambda-analysis the lambda is analyzed eagerly, so 'submit' resolves to
    // 'submit(Callable<String>): Future<String>' and the call below type-checks.
    // Without the flag it does not resolve to the Callable overload, so the call fails.
    val future = executor.submit { "OK" }

    expectFutureString(future)
}
