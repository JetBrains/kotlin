// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperHandlingOfGenericAndRawTypesInJavaOverrides

// FILE: base.kt

interface AnyGeneric<T>

interface Base<T> {
    val x: AnyGeneric<T>
    val y: AnyGeneric<T>
}

// FILE: StackJava.java
public class StackJava {
    public static abstract class KeyFace implements Base<String> {
        public AnyGeneric<String> getX() {
            return null;
        }
    }
    public static class NextFace<T> extends KeyFace {
        public AnyGeneric<String> getY() {
            return null;
        }
    }
    public static class SubjectClass extends /* raw */ NextFace {}
}

// FILE: test.kt
class MySubject : StackJava.SubjectClass() {
    fun foo() {
        val x: AnyGeneric<String> = this.x
        val y: AnyGeneric<String> = <!INITIALIZER_TYPE_MISMATCH!>this.y<!>
    }
}
