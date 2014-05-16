fun foo(s: String) {}

class A {
    fun bar(): String = ""
}

fun A.baz(x: Int) {}


fun box(): String {
    val f = "${::foo}"
    if (f != "kotlin.reflect.KFunction1<java.lang.String, kotlin.Unit>") return "Fail foo: $f"

    val nameOfA = (A() as java.lang.Object).getClass().getName()

    val g = "${A::bar}"
    if (g != "kotlin.reflect.KMemberFunction0<$nameOfA, java.lang.String>") return "Fail bar: $g"

    val h = "${A::baz}"
    if (h != "kotlin.reflect.KExtensionFunction1<$nameOfA, java.lang.Integer, kotlin.Unit>") return "Fail baz: $h"

    return "OK"
}
