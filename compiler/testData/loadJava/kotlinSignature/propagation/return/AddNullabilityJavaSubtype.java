package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;

import jet.runtime.typeinfo.KotlinSignature;

public class AddNullabilityJavaSubtype {
    @NotNull
    public CharSequence foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends AddNullabilityJavaSubtype {
        @KotlinSignature("fun String? foo()")
        public String foo() {
            throw new UnsupportedOperationException();
        }
    }
}