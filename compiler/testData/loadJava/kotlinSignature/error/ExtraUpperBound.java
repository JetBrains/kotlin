package test;

import jet.runtime.typeinfo.KotlinSignature;

public class ExtraUpperBound {
    @KotlinSignature("fun <A : Runnable> foo() : String where A : Cloneable")
    public <A extends Runnable> String foo() {
        throw new UnsupportedOperationException();
    }
}
