package test;

import java.util.List;
import java.util.Collection;

public interface InheritNullabilityGenericSubclassSimple {

    public interface Super {
        Collection<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        List<String> foo();
    }
}
