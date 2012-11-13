package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface TwoSuperclassesReturnJavaSubtype {

    public interface Super1 {
        public CharSequence foo();
    }

    public interface Super2 {
        @NotNull
        public CharSequence foo();
    }

    public interface Sub extends Super1, Super2 {
        public String foo();
    }
}