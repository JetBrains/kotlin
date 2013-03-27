package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import java.util.*;

public interface TypeParamOfClass {

    public interface Super<T> {
        @NotNull
        public T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub<T> extends Super<T> {
        public T foo();
    }
}
