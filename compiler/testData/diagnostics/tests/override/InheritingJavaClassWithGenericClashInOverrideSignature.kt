// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74148
// SCOPE_DUMP: C:foo
// FILE: A.java
public abstract class A<T> {
    public abstract Object foo(A<Object> arg0, T arg1);
}

// FILE: B.java
public abstract class B extends A<B> {
    public Object foo(A<B> arg0, B arg1) {
        return null;
    }
}

// FILE: Main.kt
<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class C<!> : B()

