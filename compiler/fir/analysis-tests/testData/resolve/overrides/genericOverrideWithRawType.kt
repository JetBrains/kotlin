// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ProperHandlingOfGenericAndRawTypesInJavaOverrides
// SCOPE_DUMP: D:foo
// FILE: A.java
public interface A<T> {
    void foo(T t);
}

// FILE: B.java
public interface B<E> extends A<E> {}

// FILE: C.java
public interface C extends B {}

// FILE: test.kt
class D : C {
    override fun foo(t: Any?) {}
}
