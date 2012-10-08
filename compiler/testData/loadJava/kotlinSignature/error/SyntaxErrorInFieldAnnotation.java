package test;

import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public class SyntaxErrorInFieldAnnotation {
    @KotlinSignature("var foo : ")
    public String foo;
}
