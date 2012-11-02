package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;

import jet.runtime.typeinfo.KotlinSignature;

public class AddNullabilitySameJavaType {
    @NotNull
    public CharSequence foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends AddNullabilitySameJavaType {
        @KotlinSignature("fun CharSequence? foo()")
        public CharSequence foo() {
            throw new UnsupportedOperationException();
        }
    }
}