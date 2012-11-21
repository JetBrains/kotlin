package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface NotNullToNullable {

    public interface Super {
        void foo(@NotNull String p);
    }

    public interface Sub extends Super {
        @ExpectLoadError("Auto type 'jet.String' is not-null, while type in alternative signature is nullable: 'String?'")
        @KotlinSignature("fun foo(p: String?)")
        void foo(String p);
    }
}