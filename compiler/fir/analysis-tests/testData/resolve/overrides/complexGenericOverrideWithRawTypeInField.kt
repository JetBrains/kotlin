// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ProperHandlingOfGenericAndRawTypesInJavaOverrides

// FILE: StackJava.java
public class StackJava {
    public interface AnyGeneric<T> {}
    public static class KeyFace {
        AnyGeneric<String> someField = null;
    }
    public static class NextFace<T> extends KeyFace {
        AnyGeneric<String> anotherField = null;
    }
    public static class SubjectClass extends /* raw */ NextFace {}
}

// FILE: test.kt
class MySubject : StackJava.SubjectClass() {
    fun foo() {
        val sf = someField     // Type should be generic
        val af = anotherField  // Type should be raw
    }
}
