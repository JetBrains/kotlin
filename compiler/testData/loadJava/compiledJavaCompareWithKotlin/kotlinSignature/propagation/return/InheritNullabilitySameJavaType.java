package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;

import jet.runtime.typeinfo.KotlinSignature;

public interface InheritNullabilitySameJavaType {

    public interface Super {
        @NotNull
        CharSequence foo();
    }

    public interface Sub extends Super {
        CharSequence foo();
    }
}