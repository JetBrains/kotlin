// KT-4827 UOE at PackageType.throwException()
// EA-53605

public interface TestInterface {
}

class C {
    inner class I {

    }
}

fun f() {
    <!NO_COMPANION_OBJECT, FUNCTION_EXPECTED!>TestInterface<!>()
    C.<!NO_COMPANION_OBJECT, FUNCTION_EXPECTED!>I<!>()
}