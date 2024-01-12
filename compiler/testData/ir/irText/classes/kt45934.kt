// TARGET_BACKEND: JVM
// SKIP_KT_DUMP

// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ K2 generates an override for foo in C, K1 does not

// FILE: kt45934.kt

class C(client: J) : I by client

// FILE: I.java

import java.util.List;

public interface I {
    <C> List<C> foo();
}

// FILE: J.java

import java.util.List;

public class J implements I {
    @Override
    public List<String> foo() {
        return null;
    }
}
