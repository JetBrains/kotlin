package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class WrongValueParametersCount {
    @ExpectLoadError("Method signature has 0 value parameters, but alternative signature has 1")
    @KotlinSignature("fun foo(a : Int) : Int")
    public Integer foo() {
        throw new UnsupportedOperationException();
    }
}
