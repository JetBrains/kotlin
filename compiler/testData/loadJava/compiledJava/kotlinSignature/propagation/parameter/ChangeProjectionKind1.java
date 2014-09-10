package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;
import java.util.*;

public interface ChangeProjectionKind1 {

    public interface Super {
        @KotlinSignature("fun foo(p: MutableList<in String>)")
        void foo(List<String> p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("Projection kind mismatch, actual: in, in alternative signature: ")
        @KotlinSignature("fun foo(p: MutableList<String>)")
        void foo(List<String> p);
    }
}
