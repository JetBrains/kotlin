// RUN_PIPELINE_TILL: BACKEND
// FILE: A.java

public class A<T> {
    public T foo(T t) {
        return t;
    }
}

// FILE: simpleFakeOverride.kt


class Some

class B : A<Some>() {
    fun test() {
        foo(Some())
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaType */
