package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class WrongFieldName {
    @ExpectLoadError("Field name mismatch, original: foo, alternative: bar")
    @KotlinSignature("var bar: String")
    public String foo;
}
