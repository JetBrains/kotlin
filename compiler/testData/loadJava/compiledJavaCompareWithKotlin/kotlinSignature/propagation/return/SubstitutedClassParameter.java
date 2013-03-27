package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

import java.lang.String;

public interface SubstitutedClassParameter {

    public interface Super<T> {
        @KotlinSignature("fun foo(): T")
        T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super<String> {
        String foo();
    }
}