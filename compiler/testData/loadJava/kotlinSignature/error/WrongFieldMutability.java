package test;

import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public class WrongFieldMutability {
    @KotlinSignature("val fooNotFinal : String")
    public String fooNotFinal;

    @KotlinSignature("var fooFinal : String")
    public final String fooFinal = "Test";
}
