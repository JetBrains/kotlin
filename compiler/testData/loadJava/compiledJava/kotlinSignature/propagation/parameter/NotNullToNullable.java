package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface NotNullToNullable {

    public interface Super {
        void foo(@NotNull String p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("Auto type 'kotlin.String' is not-null, while type in alternative signature is nullable: 'String?'")
        @KotlinSignature("fun foo(p: String?)")
        void foo(String p);
    }
}
