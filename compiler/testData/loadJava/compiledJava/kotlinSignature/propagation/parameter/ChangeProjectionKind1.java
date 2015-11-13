package test;

import java.util.*;

public interface ChangeProjectionKind1 {

    public interface Super {
        void foo(List<String> p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(List<String> p);
    }
}
