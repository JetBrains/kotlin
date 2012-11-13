package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;

import jet.runtime.typeinfo.KotlinSignature;

public interface TwoSuperclassesReturnJavaSubtype {
    public CharSequence foo();

    public interface Other {
        @NotNull
        public CharSequence foo();
    }

    public interface Sub extends TwoSuperclassesReturnJavaSubtype, Other {
        public String foo();
    }
}