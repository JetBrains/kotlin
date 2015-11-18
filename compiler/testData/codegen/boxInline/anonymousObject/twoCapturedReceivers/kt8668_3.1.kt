//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    val res = Person("OK").sayName()
    if (res != "OKsubOK")  return "fail: $res"

    return "OK"
}