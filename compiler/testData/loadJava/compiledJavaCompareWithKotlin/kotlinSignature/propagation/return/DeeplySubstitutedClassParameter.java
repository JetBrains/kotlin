package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public interface DeeplySubstitutedClassParameter {

    public interface Super<T> {
        @KotlinSignature("fun foo(): T")
        T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Middle<E> extends Super<E> {
        E foo();
    }

    public interface Sub extends Middle<String> {
        String foo();
    }
}