import test.*

public class A()

fun box(): String {
    val s = "yo".inlineMeIfYouCan<A>()().call()
    if (s !is A) return "fail"

    return "OK"
}