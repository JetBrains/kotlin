package test;

import org.jetbrains.annotations.NotNull;
import java.lang.CharSequence;

import jet.runtime.typeinfo.KotlinSignature;

public class InheritNullabilityJavaSubtype {
    @NotNull
    public CharSequence foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends InheritNullabilityJavaSubtype {
        public String foo() {
            throw new UnsupportedOperationException();
        }
    }
}