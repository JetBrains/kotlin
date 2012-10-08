package test;

import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public class NoFieldTypeRef {
    @KotlinSignature("var foo")
    public String foo;
}
