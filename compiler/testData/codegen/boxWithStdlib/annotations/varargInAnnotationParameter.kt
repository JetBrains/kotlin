import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(val s: String, vararg val p: Int)

Ann("str", 1, 2, 3) class MyClass

fun box(): String {
    val ann = javaClass<MyClass>().getAnnotation(javaClass<Ann>())
    if (ann == null) return "fail: cannot find Ann on MyClass"
    if (ann.s != "str") return "fail: annotation parameter s should be \"str\""
    if (ann.p[0] != 1) return "fail: annotation parameter p[0] should be 1"
    if (ann.p[1] != 2) return "fail: annotation parameter p[1] should be 2"
    if (ann.p[2] != 3) return "fail: annotation parameter p[2] should be 3"
    return "OK"
}

fun main(args: Array<String>) {
    println(box())
}