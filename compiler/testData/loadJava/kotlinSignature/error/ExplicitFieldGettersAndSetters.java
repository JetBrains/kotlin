package test;

import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public class ExplicitFieldGettersAndSetters {
    @KotlinSignature("var foo: String get() { return \"hello\" }")
    public String foo;
}
