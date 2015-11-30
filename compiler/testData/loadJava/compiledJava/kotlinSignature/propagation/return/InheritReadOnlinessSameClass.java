package test;

import java.util.List;

public interface InheritReadOnlinessSameClass {

    public interface Super {
        List<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        List<String> foo();
    }
}
