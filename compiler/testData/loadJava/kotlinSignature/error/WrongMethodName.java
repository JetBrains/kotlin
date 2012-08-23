package test;

import jet.runtime.typeinfo.KotlinSignature;

public class WrongMethodName {
    @KotlinSignature("fun bar() : String")
    public String foo() {
        throw new UnsupportedOperationException();
    }
}
