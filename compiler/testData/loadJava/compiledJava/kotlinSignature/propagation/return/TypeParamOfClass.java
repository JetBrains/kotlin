package test;

import org.jetbrains.annotations.NotNull;
import java.util.*;

public interface TypeParamOfClass {

    public interface Super<T> {
        @NotNull
        T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub<T> extends Super<T> {
        T foo();
    }
}
