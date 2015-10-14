// FILE: A.java

import kotlin.jvm.KotlinSignature;

interface A {
    @KotlinSignature("fun foo(kotlinSignatureName: String): Unit")
    void foo(String javaName);
}

// FILE: 1.kt

class B : A {
    override fun foo(kotlinName: String) {}
}

class C : A {
    override fun foo(kotlinSignatureName: String) {}
}
