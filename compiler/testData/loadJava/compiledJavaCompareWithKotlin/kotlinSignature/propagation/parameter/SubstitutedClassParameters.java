package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface SubstitutedClassParameters {

    public interface Super1<T> {
        @KotlinSignature("fun foo(t: T)")
        void foo(T p);
    }

    public interface Super2<E> {
        @KotlinSignature("fun foo(t: E)")
        void foo(E p);
    }

    public interface Sub extends Super1<String>, Super2<String> {
        void foo(String p);
    }
}