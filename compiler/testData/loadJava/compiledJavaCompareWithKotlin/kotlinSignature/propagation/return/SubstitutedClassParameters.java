package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public interface SubstitutedClassParameters {

    public interface Super1<T> {
        @KotlinSignature("fun foo(): T")
        T foo();
    }

    public interface Super2<E> {
        @KotlinSignature("fun foo(): E")
        E foo();
    }

    public interface Sub extends Super1<String>, Super2<String> {
        String foo();
    }
}