package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class WrongFieldInitializer {
    @ExpectLoadError("Default value is not expected in annotation for field")
    @KotlinSignature("var foo : String = \"Test\"")
    public String foo;
}
