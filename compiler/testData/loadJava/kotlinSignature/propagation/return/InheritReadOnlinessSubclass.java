package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public class InheritReadOnlinessSubclass {
    @KotlinSignature("fun foo(): Collection<String>")
    public Collection<String> foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends InheritReadOnlinessSubclass {
        public List<String> foo() {
            throw new UnsupportedOperationException();
        }
    }
}
