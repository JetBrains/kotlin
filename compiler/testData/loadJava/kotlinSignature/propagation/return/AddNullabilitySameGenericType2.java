package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;

public class AddNullabilitySameGenericType2 {
    @KotlinSignature("fun foo(): MutableList<String>")
    public List<String> foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends AddNullabilitySameGenericType2 {
        @KotlinSignature("fun foo(): MutableList<String>?")
        public List<String> foo() {
            throw new UnsupportedOperationException();
        }
    }
}