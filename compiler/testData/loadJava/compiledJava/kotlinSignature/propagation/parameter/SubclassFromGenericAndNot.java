package test;

import org.jetbrains.annotations.NotNull;

// Extracted from KT-3302, see Kt3302 test, as well
public interface SubclassFromGenericAndNot {

    public interface NonGeneric {
        void foo(@NotNull String s);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Generic<T> {
        public void foo(T key);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends NonGeneric, Generic<String> {
        @Override
        public void foo(String key);
    }
}
