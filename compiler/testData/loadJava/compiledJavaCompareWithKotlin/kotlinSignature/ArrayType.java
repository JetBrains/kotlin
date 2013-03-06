package test;

import jet.runtime.typeinfo.KotlinSignature;

public class ArrayType {
    @KotlinSignature("fun foo(): Array<String>")
    public String[] foo() {
        throw new UnsupportedOperationException();
    }
}
