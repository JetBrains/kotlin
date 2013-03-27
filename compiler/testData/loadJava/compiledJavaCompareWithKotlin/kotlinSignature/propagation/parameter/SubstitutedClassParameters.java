package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface SubstitutedClassParameters {

    public interface Super1<T> {
        @KotlinSignature("fun foo(t: T)")
        void foo(T p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Super2<E> {
        @KotlinSignature("fun foo(t: E)")
        void foo(E p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super1<String>, Super2<String> {
        void foo(String p);
    }
}