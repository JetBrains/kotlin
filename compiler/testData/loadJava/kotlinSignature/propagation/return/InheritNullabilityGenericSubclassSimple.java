package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public class InheritNullabilityGenericSubclassSimple {
    @KotlinSignature("fun foo(): MutableCollection<String>")
    public Collection<String> foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends InheritNullabilityGenericSubclassSimple {
        public List<String> foo() {
            throw new UnsupportedOperationException();
        }
    }
}
