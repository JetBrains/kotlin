package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class MissingUpperBound {
    @ExpectLoadError("Upper bound #1 for type parameter A is missing")
    @KotlinSignature("fun <A : Runnable> foo() : String")
    public <A extends Runnable & Cloneable> String foo() {
        throw new UnsupportedOperationException();
    }
}
