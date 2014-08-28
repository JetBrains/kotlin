package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import java.util.*;

public interface TypeParamOfFun {

    public interface Super {
        @NotNull
        public <T> T foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        public <E> E foo();
    }
}
