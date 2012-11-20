package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class ExtraUpperBound {
    @ExpectLoadError("Extra upper bound #1 for type parameter A")
    @KotlinSignature("fun <A : Runnable> foo() : String where A : Cloneable")
    public <A extends Runnable> String foo() {
        throw new UnsupportedOperationException();
    }
}
