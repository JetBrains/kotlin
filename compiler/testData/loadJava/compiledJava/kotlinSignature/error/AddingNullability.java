package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class AddingNullability {
    @ExpectLoadError("Auto type 'kotlin.Int' is not-null, while type in alternative signature is nullable: 'Int?'")
    @KotlinSignature("fun foo() : Int?")
    public int foo() {
        throw new UnsupportedOperationException();
    }
}
