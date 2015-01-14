package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class MissingUpperBound {
    @ExpectLoadError("Upper bound number mismatch for A. Expected 2, but found 1")
    @KotlinSignature("fun <A : Runnable> foo() : String")
    public <A extends Runnable & Cloneable> String foo() {
        throw new UnsupportedOperationException();
    }
}
