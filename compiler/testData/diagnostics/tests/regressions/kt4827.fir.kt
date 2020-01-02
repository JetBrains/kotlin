// KT-4827 UOE at PackageType.throwException()
// EA-53605

public interface TestInterface {
}

class C {
    inner class I {

    }
}

fun f() {
    <!UNRESOLVED_REFERENCE!>TestInterface<!>()
    C.<!UNRESOLVED_REFERENCE!>I<!>()
}
