package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public interface DeeplySubstitutedClassParameter2 {

    public interface Super<T> {
        @KotlinSignature("fun foo(): T")
        T foo();
    }

    public interface Middle<E> extends Super<E> {
    }

    public interface Sub extends Middle<String> {
        String foo();
    }
}