//NO_CHECK_LAMBDA_INLINING
import test.*

val x: () -> String = foo<String>()

fun box(): String {
    return x()
}