package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;

import jet.runtime.typeinfo.KotlinSignature;

public interface TwoSuperclassesReturnSameJavaType {
    public CharSequence foo();

    public interface Other {
        @NotNull
        public CharSequence foo();
    }

    public interface Sub extends TwoSuperclassesReturnSameJavaType, Other {
        public CharSequence foo();
    }
}