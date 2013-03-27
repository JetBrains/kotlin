package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface DeeplySubstitutedClassParameter {

    public interface Super<T> {
        @KotlinSignature("fun foo(t: T)")
        void foo(T p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Middle<E> extends Super<E> {
        void foo(E p);
    }

    public interface Sub extends Middle<String> {
        void foo(String p);
    }
}