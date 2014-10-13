package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface AddNullabilitySameJavaType {

    public interface Super {
        @NotNull
        CharSequence foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("Auto type 'kotlin.CharSequence' is not-null, while type in alternative signature is nullable: 'CharSequence?'")
        @KotlinSignature("fun foo(): CharSequence?")
        CharSequence foo();
    }
}
