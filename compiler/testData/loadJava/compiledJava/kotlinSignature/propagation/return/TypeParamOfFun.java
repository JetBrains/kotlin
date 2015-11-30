package test;

import org.jetbrains.annotations.NotNull;
import java.util.*;

public interface TypeParamOfFun {

    public interface Super {
        @NotNull
        <T> T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        <E> E foo();
    }
}
