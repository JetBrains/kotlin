// KT-4827 UOE at PackageType.throwException()
// EA-53605

public trait TestInterface {
}

class C {
    inner class I {

    }
}

fun f() {
    <!NO_CLASS_OBJECT, FUNCTION_EXPECTED!>TestInterface<!>()
    C.<!UNRESOLVED_REFERENCE!>I<!>()
}