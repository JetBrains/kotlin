package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import java.util.*;

public interface TypeParamOfFun {

    public interface Super {
        @NotNull
        public <T> T foo();
    }

    public interface Sub extends Super {
        public <E> E foo();
    }
}
