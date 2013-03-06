package test;

import java.lang.String;
import jet.runtime.typeinfo.KotlinSignature;

public class PropertySimpleType {
    @KotlinSignature("var fieldOne : String")
    public String fieldOne;

    @KotlinSignature("var fieldTwo : String?")
    public String fieldTwo;
}
