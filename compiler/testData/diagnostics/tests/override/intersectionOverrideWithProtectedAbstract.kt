// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
public abstract class A {
    protected abstract fun bar(): String
}

public interface B {
    public fun bar(): String
}

public abstract class C: A(), B
