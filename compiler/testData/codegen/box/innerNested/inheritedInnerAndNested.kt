// TARGET_BACKEND: JVM_IR
// ISSUE: KT-65333

// FILE: BaseClass.java
public class BaseClass {
    public class Inner {
        public String box() {
            return "BaseClass";
        }
    }
}

// FILE: BaseInterface.java
public interface BaseInterface {
    class Inner {
        public String box() {
            return "BaseInterface";
        }
    }
}

// FILE: main.kt
class A: BaseClass(), BaseInterface
class B : BaseClass(), BaseInterface {
    class Inner {
        fun box(): String = "B"
    }
}
class C : BaseClass(), BaseInterface {
    inner class Inner {
        fun box(): String = "C"
    }
}
object D : BaseClass(), BaseInterface
object E : BaseClass(), BaseInterface {
    class Inner {
        fun box(): String = "E"
    }
}

object F : BaseClass()
object G : BaseClass() {
    class Inner {
        fun box(): String = "G"
    }
}
object H : BaseInterface
object I : BaseInterface {
    class Inner {
        fun box(): String = "I"
    }
}

fun check(actual: String, expected: String) {
    if (expected != actual) {
        throw AssertionError("\nExpected: $expected\nActual: $actual\n")
    }
}

fun box(): String {
    check(A().Inner().box(), "BaseClass")
//    check(A.Inner().box(), "BaseInterface")
//    check(B().Inner().box(), "BaseInterface")
    check(B.Inner().box(), "B")
    check(C().Inner().box(), "C")
//    check(C.Inner().box(), "BaseInterface")
    check(D.Inner().box(), "BaseClass")
    check(E.Inner().box(), "E")
    check(F.Inner().box(), "BaseClass")
    check(G.Inner().box(), "G")
//    check(H.Inner().box(), "E")
    check(I.Inner().box(), "I")
    return "OK"
}
