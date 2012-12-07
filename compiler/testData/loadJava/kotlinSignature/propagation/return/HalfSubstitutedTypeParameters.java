package test;

import java.util.*;
import org.jetbrains.annotations.NotNull;
import jet.runtime.typeinfo.KotlinSignature;

public interface HalfSubstitutedTypeParameters {

    public interface TrickyList<X, E> extends List<E> {}

    public interface Super {
        @KotlinSignature("fun foo(): MutableList<String?>")
        List<String> foo();
    }

    public interface Sub extends Super {
        TrickyList<Integer, String> foo();
    }
}
