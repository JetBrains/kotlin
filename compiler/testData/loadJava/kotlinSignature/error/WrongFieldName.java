package test;

import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public class WrongFieldName {
    @KotlinSignature("val bar: String")
    public String foo;
}
