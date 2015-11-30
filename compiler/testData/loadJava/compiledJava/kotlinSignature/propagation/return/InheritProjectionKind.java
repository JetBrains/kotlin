package test;

import java.util.List;
import java.util.Collection;

public interface InheritProjectionKind {

    public interface Super {
        Collection<Number> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        List<Number> foo();
    }
}
