// FILE: a.kt
class A {
    class B {
        class C
    }
}

// FILE: b.kt
import A.B
import A.B.C

val a = A()
val b = B()
val ab = A.B()
val c = C()
val bc = B.C()
val abc = A.B.C()
