import A.B
import A.B.C

class A {
    class B {
        class C
    }
}

fun box(): String {
    val a = A()
    val b = B()
    val ab = A.B()
    val c = C()
    val bc = B.C()
    val abc = A.B.C()
    return "OK"
}
