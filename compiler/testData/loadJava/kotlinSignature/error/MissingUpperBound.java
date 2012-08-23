package test;

import jet.runtime.typeinfo.KotlinSignature;

public class MissingUpperBound {
    @KotlinSignature("fun <A : Runnable> foo() : String")
    public <A extends Runnable & Cloneable> String foo() {
        throw new UnsupportedOperationException();
    }
}
