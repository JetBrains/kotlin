package test;

import java.util.List;
import java.util.Collection;

public interface InheritReadOnlinessOfArgument {

    public interface Super {
        List<List<String>> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        List<List<String>> foo();
    }
}
