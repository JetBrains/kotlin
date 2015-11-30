package test;

import java.util.Collection;

public interface SameProjectionKind {

    public interface Super {
        Collection<? extends Number> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        Collection<? extends Number> foo();
    }
}
