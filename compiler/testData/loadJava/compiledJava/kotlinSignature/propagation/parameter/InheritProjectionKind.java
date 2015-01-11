//ALLOW_AST_ACCESS
package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;
import java.util.*;

public interface InheritProjectionKind {

    public interface Super {
        @KotlinSignature("fun foo(p: MutableList<in String>)")
        void foo(List<String> p);

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        void foo(List<String> p);
    }
}
