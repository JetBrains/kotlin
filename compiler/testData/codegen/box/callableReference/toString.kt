fun foo(s: String) {}

class A {
    fun bar(): String = ""
}

fun A.baz() {}


fun box(): String {
    val f = "${::foo}"
    if (f != "kotlin.reflect.KFunctionImpl1<java.lang.String, kotlin.Unit>") return "Fail foo: $f"

    val nameOfA = (A() as java.lang.Object).getClass().getName()

    val g = "${A::bar}"
    if (g != "kotlin.reflect.KMemberFunctionImpl0<$nameOfA, java.lang.String>") return "Fail bar: $g"

    val h = "${A::baz}"
    if (h != "kotlin.reflect.KExtensionFunctionImpl0<$nameOfA, kotlin.Unit>") return "Fail baz: $h"

    return "OK"
}
