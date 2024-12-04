// JAVAC_EXPECTED_FILE
// LANGUAGE: +TypeEnhancementImprovementsInStrictMode

package test;

import org.jetbrains.annotations.*;

public class Basic<T extends @NotNull Object> implements G2<@NotNull T, @NotNull String> {
    interface G<T> { }

    interface G2<A, B> { }

    public interface MyClass<TT> {
        void f1(G<@NotNull String> p);
        G2<@Nullable String, @NotNull Integer> f2();
        <T extends @NotNull Object> void f3(@NotNull T x) { };
    }
}
