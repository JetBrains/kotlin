// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78819

// FILE: A.java

public abstract class A implements I0 {
    @Override
    public <T3, T4 extends T3> Object func(T4 c) {
        return null;
    }
}

// FILE: use.kt

interface I0 {
    fun <T3, T4 : T3> func(c: T4): Any
}

open class C : A()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, javaType, nullableType,
typeConstraint, typeParameter */
