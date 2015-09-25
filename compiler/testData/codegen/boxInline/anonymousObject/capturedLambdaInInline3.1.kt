//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    val bar1 = bar {"123"} ()
    val bar2 = bar2 { "1234" } ()
    return if (bar1 == "123" && bar2 == "1234") "OK" else "fail: $bar1 $bar2"
}

inline fun bar2(crossinline y: () -> String) = {
    { { call(y) }() }()
}
