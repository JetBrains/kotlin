package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class NoFieldTypeRef {
    @ExpectLoadError("Field annotation for shouldn't have type reference")
    @KotlinSignature("var foo")
    public String foo;
}
