// KT-4827 UOE at PackageType.throwException()
// EA-53605

public interface TestInterface {
}

class C {
    inner class I {

    }
}

fun f() {
    <!INTERFACE_AS_FUNCTION!>TestInterface<!>()
    C.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>I<!>()
}
