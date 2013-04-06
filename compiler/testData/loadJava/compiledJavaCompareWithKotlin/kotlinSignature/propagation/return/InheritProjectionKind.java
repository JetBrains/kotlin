package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public interface InheritProjectionKind {

    public interface Super {
        @KotlinSignature("fun foo(): MutableCollection<out Number>")
        Collection<Number> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        List<Number> foo();
    }
}
