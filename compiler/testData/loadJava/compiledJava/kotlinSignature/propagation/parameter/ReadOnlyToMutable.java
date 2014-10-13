package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;
import java.util.*;

public interface ReadOnlyToMutable {

    public interface Super {
        @KotlinSignature("fun foo(p: List<String>)")
        void foo(List<String> p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("Parameter type changed for method which overrides another: MutableList<String>, was: List<String>")
        @KotlinSignature("fun foo(p: MutableList<String>)")
        void foo(List<String> p);
    }
}
