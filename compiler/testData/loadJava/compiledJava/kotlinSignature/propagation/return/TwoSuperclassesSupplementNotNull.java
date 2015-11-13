package test;

import java.util.*;

public interface TwoSuperclassesSupplementNotNull {

    public interface Super1 {
        List<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Super2 {
        List<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super1, Super2 {
        List<String> foo();
    }
}
