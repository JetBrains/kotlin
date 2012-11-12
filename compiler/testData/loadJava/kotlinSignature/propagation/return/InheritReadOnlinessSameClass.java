package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public class InheritReadOnlinessSameClass {
    @KotlinSignature("fun foo(): List<String>")
    public List<String> foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends InheritReadOnlinessSameClass {
        public List<String> foo() {
            throw new UnsupportedOperationException();
        }
    }
}
