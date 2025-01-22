// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ProperHandlingOfGenericAndRawTypesInJavaOverrides
// SCOPE_DUMP: D:foo
// FILE: A.java
public interface A<T> {
    void foo(T t);
}

// FILE: B.java
public interface B<E> extends A<E> {}

// FILE: Z.java
public abstract class Z {
    public void foo(Object t) {}
}

// FILE: C.java
public abstract class C extends Z implements B {}

// FILE: test.kt
class D : C()
