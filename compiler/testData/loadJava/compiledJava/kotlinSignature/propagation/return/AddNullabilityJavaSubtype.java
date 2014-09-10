package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface AddNullabilityJavaSubtype {

    public interface Super {
        @NotNull
        CharSequence foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("Auto type 'kotlin.String' is not-null, while type in alternative signature is nullable: 'String?'")
        @KotlinSignature("fun foo(): String?")
        String foo();
    }
}
