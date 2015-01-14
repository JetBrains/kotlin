package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class WrongFieldMutability {
    @ExpectLoadError("Wrong mutability in annotation for field")
    @KotlinSignature("val fooNotFinal : String")
    public String fooNotFinal;

    @ExpectLoadError("Wrong mutability in annotation for field")
    @KotlinSignature("var fooFinal : String")
    public final String fooFinal = "Test";
}
