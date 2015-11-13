package test;

import org.jetbrains.annotations.NotNull;
import java.util.*;

public interface ReadOnlyToMutable {

    public interface Super {
        void foo(List<String> p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(List<String> p);
    }
}
