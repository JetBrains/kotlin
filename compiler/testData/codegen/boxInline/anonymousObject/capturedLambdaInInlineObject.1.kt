//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    return bar { "OK" }.run()
}