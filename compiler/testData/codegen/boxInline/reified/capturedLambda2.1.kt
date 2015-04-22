//NO_CHECK_LAMBDA_INLINING
import test.*

inline fun <reified R> foo() = bar<R, String>() {"OK"}

fun box(): String {
    return foo<String>()()
}