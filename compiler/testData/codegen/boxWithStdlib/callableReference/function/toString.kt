fun foo(s: String) {}

class A {
    fun bar(): String = ""
}

fun A.baz() {}


fun box(): String {
    val f = "${::foo}"
    if (f != "kotlin.reflect.jvm.internal.KFunction1Impl<java.lang.String, kotlin.Unit>") return "Fail foo: $f"

    val nameOfA = (A() as java.lang.Object).getClass().getName()

    val g = "${A::bar}"
    if (g != "kotlin.reflect.jvm.internal.KMemberFunction0Impl<$nameOfA, java.lang.String>") return "Fail bar: $g"

    val h = "${A::baz}"
    if (h != "kotlin.reflect.jvm.internal.KExtensionFunction0Impl<$nameOfA, kotlin.Unit>") return "Fail baz: $h"

    return "OK"
}
