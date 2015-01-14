package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class ExtraUpperBound {
    @ExpectLoadError("Upper bound number mismatch for A. Expected 1, but found 2")
    @KotlinSignature("fun <A : Runnable> foo() : String where A : Cloneable")
    public <A extends Runnable> String foo() {
        throw new UnsupportedOperationException();
    }
}
