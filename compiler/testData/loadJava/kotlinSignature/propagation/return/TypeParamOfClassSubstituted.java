package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import java.util.*;

public interface TypeParamOfClassSubstituted {

    public interface Super<T> {
        @NotNull
        public T foo();
    }

    public interface Sub extends Super<String> {
        public String foo();
    }
}
