package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface SubstitutedClassParameter {

    public interface Super<T> {
        @KotlinSignature("fun foo(t: T)")
        void foo(T p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super<String> {
        void foo(String p);
    }
}