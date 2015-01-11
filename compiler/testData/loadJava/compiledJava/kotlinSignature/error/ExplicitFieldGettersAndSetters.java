package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class ExplicitFieldGettersAndSetters {
    @ExpectLoadError("Field annotation for shouldn't have getters and setters")
    @KotlinSignature("var foo: String get() { return \"hello\" }")
    public String foo;
}
