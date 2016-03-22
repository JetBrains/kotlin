public class ClassObjectField {
    public static void foo() {
        k.ClassWithClassObject.Co<caret>mpanion.f();
    }
}

// REF: companion object of (k).ClassWithClassObject
// CLS_REF: companion object of (k).ClassWithClassObject
