package test;

import jet.runtime.typeinfo.KotlinSignature;

public class SyntaxError {
    @KotlinSignature("fun foo(,) : Int")
    public Integer foo() {
        throw new UnsupportedOperationException();
    }
}
