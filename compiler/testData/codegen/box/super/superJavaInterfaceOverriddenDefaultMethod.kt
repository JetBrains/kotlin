// LANGUAGE: +AllowSuperCallToJavaInterface
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// FILE: I.java
public interface I {
    default int f() { return 1; }
}

// FILE: J.java
public interface J extends I {
    default int f() { return 2; }
}

// FILE: box.kt
interface K : I, J {
    fun i_f(): Int = super<I>.f()
    fun j_f(): Int = super<J>.f()
}

class C : K

fun box(): String {
    if (C().i_f() != 1) return "Fail C.i_f"
    if (C().j_f() != 2) return "Fail C.j_f"
    if (C().f() != 2) return "Fail C.f"

    return "OK"
}
