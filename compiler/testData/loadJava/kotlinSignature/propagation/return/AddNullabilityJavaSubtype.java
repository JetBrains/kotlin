package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface AddNullabilityJavaSubtype {

    public interface Super {
        @NotNull
        CharSequence foo();
    }

    public interface Sub extends Super {
        @KotlinSignature("fun String? foo()")
        String foo();
    }
}