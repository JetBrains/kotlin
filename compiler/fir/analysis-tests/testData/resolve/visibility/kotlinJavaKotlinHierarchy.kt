// RUN_PIPELINE_TILL: BACKEND
// FILE: C.kt

class C : B() {
    override fun foo() {}
}

// FILE: B.java

public class B extends A {
    @java.lang.Override
    public void foo() {}
}

// FILE: A.kt

abstract class A {
    abstract fun foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, override */
