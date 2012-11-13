package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public class InheritVariance {
    @KotlinSignature("fun foo(): MutableCollection<out Number>")
    public Collection<Number> foo() {
        throw new UnsupportedOperationException();
    }

    public class Sub extends InheritVariance {
        public List<Number> foo() {
            throw new UnsupportedOperationException();
        }
    }
}
