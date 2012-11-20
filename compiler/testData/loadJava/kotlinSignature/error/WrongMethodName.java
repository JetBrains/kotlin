package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class WrongMethodName {
    @ExpectLoadError("Function names mismatch, original: foo, alternative: bar")
    @KotlinSignature("fun bar() : String")
    public String foo() {
        throw new UnsupportedOperationException();
    }
}
