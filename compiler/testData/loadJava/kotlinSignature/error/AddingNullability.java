package test;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class AddingNullability {
    @ExpectLoadError("Auto type 'jet.Int' is not-null, while type in alternative signature is nullable: 'Int?'")
    @KotlinSignature("fun foo() : Int?")
    public int foo() {
        throw new UnsupportedOperationException();
    }
}
