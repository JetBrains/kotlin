// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-24239

// FILE: StackJava.java
public class StackJava {
    interface AnyGeneric<T> {}
    interface KeyFace {
        <T extends AnyGeneric<?>> void anyMethod(AnyGeneric<T> p);
    }
    interface NextFace<T extends KeyClass> extends KeyFace {}
    public abstract static class KeyClass implements KeyFace {
        @Override public <T extends AnyGeneric<?>> void anyMethod(AnyGeneric<T> p) {}
    }
    public static class SubjectClass extends KeyClass implements NextFace {}
}

// FILE: test.kt
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class MySubject<!> : StackJava.SubjectClass()
