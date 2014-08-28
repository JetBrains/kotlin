package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class SyntaxErrorInFieldAnnotation {
    @ExpectLoadError("Alternative signature has syntax error at 10: Type expected")
    @KotlinSignature("var foo : ")
    public String foo;
}
