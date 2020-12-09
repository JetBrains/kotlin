// !LANGUAGE: +ImprovementsAroundTypeEnhancement

package test;

import org.jetbrains.annotations.*;

public class Basic {
    interface G<T> {
    }

    interface G2<A, B> {
    }

    public interface MyClass<TT> {
        void f(G<@NotNull String> p);
        void f(G2<@Nullable String, @NotNull Integer> p);
    }
}
