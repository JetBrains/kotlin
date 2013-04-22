fun foo(s: String) {}

class A {
    fun bar(): String = ""
}

fun A.baz() {}


fun box(): String {
    val f = "${::foo}"
    if (f != "jet.KFunctionImpl1<? super java.lang.String, ? extends jet.Unit>") return "Fail foo: $f"

    val g = "${A::bar}"
    if (g != "jet.KMemberFunctionImpl0<? super A, ? extends java.lang.String>") return "Fail bar: $f"

    val h = "${A::baz}"
    if (h != "jet.KExtensionFunctionImpl0<? super A, ? extends jet.Unit>") return "Fail baz: $h"

    return "OK"
}
