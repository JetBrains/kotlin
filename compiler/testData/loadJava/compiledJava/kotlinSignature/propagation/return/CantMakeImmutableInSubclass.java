package test;

import java.util.*;

public interface CantMakeImmutableInSubclass {

    public interface Super {
        Collection<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        List<String> foo();
    }
}
