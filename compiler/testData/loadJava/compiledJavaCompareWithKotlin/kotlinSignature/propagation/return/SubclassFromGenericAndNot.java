package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface SubclassFromGenericAndNot {

    public interface NonGeneric {
        String foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Generic<T> {
        @KotlinSignature("fun foo(): T")
        public T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends NonGeneric, Generic<String> {
        @Override
        public String foo();
    }
}
