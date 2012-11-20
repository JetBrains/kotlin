package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface AddNullabilityJavaSubtype {

    public interface Super {
        @NotNull
        CharSequence foo();
    }

    public interface Sub extends Super {
        @ExpectLoadError("Alternative signature has 2 syntax errors, first is at 10: Expecting '(")
        @KotlinSignature("fun String? foo()")
        String foo();
    }
}