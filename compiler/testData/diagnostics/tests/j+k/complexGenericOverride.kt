// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-24239

// FILE: StackJava.java
public class StackJava {
    interface AnyGeneric<T> {}
    interface KeyFace {
        <F extends AnyGeneric<?>> void anyMethod(AnyGeneric<F> p);
    }
    interface NextFace<N extends KeyClass> extends KeyFace {
        <N extends AnyGeneric<?>> void bar(AnyGeneric<N> p);
    }
    public abstract static class KeyClass implements KeyFace {
        @Override public <C extends AnyGeneric<?>> void anyMethod(AnyGeneric<C> p) {}
    }

    public static class Some implements AnyGeneric<Some> {}
    public static class Another implements AnyGeneric<Another> {}
    public static class Incorrect implements AnyGeneric<String> {}

    public static class SubjectClass extends KeyClass implements NextFace {
        @Override public <S extends AnyGeneric<?>> void bar(AnyGeneric<S> p) {}
    }
}

// FILE: test.kt
<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class MySubject<!> : StackJava.SubjectClass() {
    fun foo() {
        this.anyMethod<StackJava.Some>(StackJava.Some());
        this.anyMethod<StackJava.Some>(StackJava.Another());
        this.anyMethod<StackJava.Some>(StackJava.Incorrect());
        this.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>anyMethod<!>(StackJava.Incorrect())
        this.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>(StackJava.Incorrect())
        this.anyMethod<StackJava.Incorrect>(StackJava.Incorrect())
        this.bar<StackJava.Incorrect>(StackJava.Incorrect())
    }
}
