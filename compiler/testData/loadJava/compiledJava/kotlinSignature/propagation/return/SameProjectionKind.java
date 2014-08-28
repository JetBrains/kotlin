package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public interface SameProjectionKind {

    public interface Super {
        Collection<? extends Number> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        Collection<? extends Number> foo();
    }
}
