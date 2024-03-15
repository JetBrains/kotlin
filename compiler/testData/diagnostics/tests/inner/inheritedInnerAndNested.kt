// DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED
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


fun test() {
    A().Inner().box()
    A.<!UNRESOLVED_REFERENCE!>Inner<!>().box() // should be an error
    B().<!UNRESOLVED_REFERENCE!>Inner<!>().box() // should be an error
    B.Inner().box()
    C().Inner().box()
    C.<!RESOLUTION_TO_CLASSIFIER!>Inner<!>().box() // should be an error
    D.Inner().box()
    E.Inner().box()
    F.Inner().box()
    G.Inner().box()
    H.<!UNRESOLVED_REFERENCE!>Inner<!>().box() // should be an error
    I.Inner().box()
}
