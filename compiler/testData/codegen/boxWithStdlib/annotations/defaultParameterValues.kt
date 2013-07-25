import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(
        val i: Int = 1,
        val s: String = "a",
        val a: Ann2 = Ann2(),
        val e: MyEnum = MyEnum.A,
        val c: Class<*> = javaClass<A>(),
        val ia: IntArray = intArray(1, 2),
        val sa: Array<String> = array("a", "b")
)

fun box(): String {
    val ann = javaClass<MyClass>().getAnnotation(javaClass<Ann>())
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.i != 1) return "fail: annotation parameter i should be 1, but was ${ann.i}"
    if (ann.s != "a") return "fail: annotation parameter s should be \"a\", but was ${ann.s}"
    if (ann.a.toString() != "@Ann2()") return "fail: annotation parameter a should be of class Ann2, but was ${ann.a}"
    if (ann.e != MyEnum.A) return "fail: annotation parameter e should be MyEnum.A, but was ${ann.e}"
    if (ann.c != javaClass<A>()) return "fail: annotation parameter c should be of class A, but was ${ann.c}"
    if (ann.ia[0] != 1 || ann.ia[1] != 2) return "fail: annotation parameter ia should be [1, 2], but was ${ann.ia}"
    if (ann.sa[0] != "a" || ann.sa[1] != "b") return "fail: annotation parameter ia should be [\"a\", \"b\"], but was ${ann.sa}"
    return "OK"
}

annotation class Ann2

enum class MyEnum {
    A
}

class A

Ann class MyClass
