package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface NullableToNotNullKotlinSignature {

    public interface Super {
        @KotlinSignature("fun foo(p: String?)")
        void foo(String p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("Parameter type changed for method which overrides another: String, was: String?")
        @KotlinSignature("fun foo(p: String)")
        void foo(String p);
    }
}
