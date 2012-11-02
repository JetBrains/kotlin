package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;

import jet.runtime.typeinfo.KotlinSignature;

public class InheritNullabilitySameJavaType {
    @NotNull
    public CharSequence foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends InheritNullabilitySameJavaType {
        public CharSequence foo() {
            throw new UnsupportedOperationException();
        }
    }
}