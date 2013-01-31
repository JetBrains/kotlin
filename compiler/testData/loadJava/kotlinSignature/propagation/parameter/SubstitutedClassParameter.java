package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface SubstitutedClassParameter {

    public interface Super<T> {
        @KotlinSignature("fun foo(t: T)")
        void foo(T p);
    }

    public interface Sub extends Super<String> {
        void foo(String p);
    }
}