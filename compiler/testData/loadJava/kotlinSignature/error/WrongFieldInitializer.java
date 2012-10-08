package test;

import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public class WrongFieldInitializer {
    @KotlinSignature("var foo : String = \"Test\"")
    public String foo;
}
