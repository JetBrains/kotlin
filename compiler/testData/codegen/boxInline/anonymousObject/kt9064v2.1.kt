//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {

    val test = Test("OK")

    return test._parameter.property.property
}