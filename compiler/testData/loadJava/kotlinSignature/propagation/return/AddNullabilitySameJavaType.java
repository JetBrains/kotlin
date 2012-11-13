package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;
import jet.runtime.typeinfo.KotlinSignature;

public interface AddNullabilitySameJavaType {

    public interface Super {
        @NotNull
        CharSequence foo();
    }

    public interface Sub extends Super {
        @KotlinSignature("fun CharSequence? foo()")
        CharSequence foo();
    }
}