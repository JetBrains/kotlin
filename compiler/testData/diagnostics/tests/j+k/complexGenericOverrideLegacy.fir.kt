// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProperHandlingOfGenericAndRawTypesInJavaOverrides
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
class MySubject : StackJava.SubjectClass() {
    fun foo() {
        this.anyMethod<StackJava.Some>(StackJava.Some());
        this.anyMethod<StackJava.Some>(<!ARGUMENT_TYPE_MISMATCH!>StackJava.Another()<!>);
        this.anyMethod<StackJava.Some>(<!ARGUMENT_TYPE_MISMATCH!>StackJava.Incorrect()<!>);
        this.<!CANNOT_INFER_PARAMETER_TYPE!>anyMethod<!>(<!ARGUMENT_TYPE_MISMATCH!>StackJava.Incorrect()<!>)
        this.<!CANNOT_INFER_PARAMETER_TYPE!>bar<!>(<!ARGUMENT_TYPE_MISMATCH!>StackJava.Incorrect()<!>)
        this.anyMethod<StackJava.Incorrect>(<!ARGUMENT_TYPE_MISMATCH!>StackJava.Incorrect()<!>)
        this.bar<StackJava.Incorrect>(<!ARGUMENT_TYPE_MISMATCH!>StackJava.Incorrect()<!>)
    }
}
