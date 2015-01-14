package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class SyntaxError {
    @ExpectLoadError("Alternative signature has 2 syntax errors, first is at 8: Expecting a parameter declaration")
    @KotlinSignature("fun foo(,) : Int")
    public Integer foo() {
        throw new UnsupportedOperationException();
    }
}
