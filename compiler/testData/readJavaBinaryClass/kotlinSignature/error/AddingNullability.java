package test;

import jet.runtime.typeinfo.KotlinSignature;

public class AddingNullability {
    @KotlinSignature("fun foo() : Int?")
    public int foo() {
        throw new UnsupportedOperationException();
    }
}
