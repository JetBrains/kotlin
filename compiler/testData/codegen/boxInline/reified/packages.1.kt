//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    val x = foo1<Int>().javaClass.getGenericSuperclass()?.toString()
    if (x != "test.A<java.lang.Integer>") return "fail 1: " + x

    if (!foo2<String>("abc")) return "fail 2"
    if (foo2<Int>("abc")) return "fail 3"

    if (!foo3<String>("abc", "cde")) return "fail 4"
    if (foo3<String>("abc", 1)) return "fail 5"

    return "OK"
}
