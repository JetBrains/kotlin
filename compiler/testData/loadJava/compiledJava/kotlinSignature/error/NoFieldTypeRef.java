package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class NoFieldTypeRef {
    @ExpectLoadError("Field annotation for shouldn't have type reference")
    @KotlinSignature("var foo")
    public String foo;
}
