package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

// Extracted from KT-3302, see Kt3302 test, as well
public interface SubclassFromGenericAndNot {

    public interface NonGeneric {
        void foo(@NotNull String s);
    }

    public interface Generic<T> {
        @KotlinSignature("fun foo(key : T)")
        public void foo(T key);
    }

    public interface Sub extends NonGeneric, Generic<String> {
        @Override
        public void foo(String key);
    }
}
