package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class ReturnTypeMissing {
    @ExpectLoadError("Return type in alternative signature is missing, while in real signature it is 'jet.Int'")
    @KotlinSignature("fun foo(a : String)")
    public int foo(String a) {
        throw new UnsupportedOperationException();
    }
}
