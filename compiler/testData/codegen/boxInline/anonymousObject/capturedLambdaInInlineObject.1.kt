//NO_CHECK_LAMBDA_INLINING
import test.*

internal fun box(): String {
    return bar { "OK" }.run()
}