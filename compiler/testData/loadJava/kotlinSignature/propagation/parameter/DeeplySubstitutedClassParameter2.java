package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface DeeplySubstitutedClassParameter2 {

    public interface Super<T> {
        @KotlinSignature("fun foo(t: T)")
        void foo(T p);
    }

    public interface Middle<E> extends Super<E> {
    }

    public interface Sub extends Middle<String> {
        void foo(String p);
    }
}