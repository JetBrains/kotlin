import java.lang.annotation.*

[Retention(RetentionPolicy.RUNTIME)]
annotation class Ann(val s: String)

Ann(Derived.FOO) class C

fun box(): String {
    val ann = javaClass<C>().getAnnotation(javaClass<Ann>())!!
    if (ann.s != "FOO") return "Fail: ${ann.s}"
    return "OK"
}