package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;

import jet.runtime.typeinfo.KotlinSignature;

public class AddNotNullJavaSubtype {
    public CharSequence foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends AddNotNullJavaSubtype {
        @NotNull
        public String foo() {
            throw new UnsupportedOperationException();
        }
    }
}