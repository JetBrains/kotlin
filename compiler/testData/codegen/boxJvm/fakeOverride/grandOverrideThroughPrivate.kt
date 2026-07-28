// MODULE: lib1v1
open class A
// MODULE: lib2v1(lib1v1)
open class B : A()
// MODULE: lib3v1(lib2v1, lib1v1)
class C : A()

// MODULE: lib1v2
open class A {
    open fun foo() = "A"
}
// MODULE: lib2v2(lib1v1)
open class B : A() {
    private fun foo() = "B"
}
// MODULE: lib3v2(lib1v2, lib2v1)
class C : B() {
    override fun foo() = "C"
}

// MODULE: main(lib1v2, lib2v2, lib3v2)
import kotlin.test.*

fun box(): String {
    val resA = test(A())
    if (resA != "A") return "A: " + resA
    val resB = test(B())
    if (resB != "A") return "B: " + resB
    val resC = test(C())
    if (resC != "C") return "C: " + resC
    return "OK"
}

private fun test(x: A): String = x.foo()
