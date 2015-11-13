package test;

import java.util.*;

public interface TwoSuperclassesConflictingProjectionKinds {

    public interface Super1 {
        Collection<CharSequence> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Super2 {
        Collection<CharSequence> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super1, Super2 {
        Collection<CharSequence> foo();
    }
}
