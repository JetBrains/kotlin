package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public interface InheritNullabilityGenericSubclassSimple {

    public interface Super {
        @KotlinSignature("fun foo(): MutableCollection<String>")
        Collection<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        List<String> foo();
    }
}
