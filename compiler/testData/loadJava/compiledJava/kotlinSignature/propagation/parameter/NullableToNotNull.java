package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface NullableToNotNull {

    public interface Super {
        void foo(String p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("In superclass type is nullable: [String?], in subclass it is not: String")
        void foo(@NotNull String p);
    }
}
