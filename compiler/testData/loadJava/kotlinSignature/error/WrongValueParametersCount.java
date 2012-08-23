package test;

import jet.runtime.typeinfo.KotlinSignature;

public class WrongValueParametersCount {
    @KotlinSignature("fun foo(a : Int) : Int")
    public Integer foo() {
        throw new UnsupportedOperationException();
    }
}
