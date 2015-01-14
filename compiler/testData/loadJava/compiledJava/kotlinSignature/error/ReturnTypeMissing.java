package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class ReturnTypeMissing {
    @ExpectLoadError("Return type in alternative signature is missing, while in real signature it is 'kotlin.Int'")
    @KotlinSignature("fun foo(a : String)")
    public int foo(String a) {
        throw new UnsupportedOperationException();
    }
}
