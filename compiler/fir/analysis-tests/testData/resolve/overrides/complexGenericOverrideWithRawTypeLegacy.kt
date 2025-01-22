// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ProperHandlingOfGenericAndRawTypesInJavaOverrides
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
    public static class SubjectClass extends KeyClass implements /* raw */ NextFace {}
}

// FILE: test.kt
class MySubject : StackJava.SubjectClass()
