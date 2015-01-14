package test;

import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;
import java.util.*;
import java.util.List;

public interface InheritMutability {

    public interface Super {
        @KotlinSignature("fun <A: MutableList<String>> foo(a: A)")
        <A extends List<String>> void foo(A a);
    }

    public interface Sub extends Super {
        <B extends List<String>> void foo(B b);
    }
}
