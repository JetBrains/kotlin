// FILE: A.java
// ANDROID_ANNOTATIONS
import kotlin.annotations.jvm.internal.*;


public class A {
    public void foo(@ParameterName("hello") String world) {}
}

// FILE: B.kt

class B : A() {
    override fun foo(hello: String) {}
}

// FILE: C.kt

class C : A() {
}

