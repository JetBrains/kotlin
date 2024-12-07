// LANGUAGE: -TypeEnhancementImprovementsInStrictMode

package test;

import org.jetbrains.annotations.*;

public class Basic_DisabledImprovements {
    public interface G<@NotNull T> {
        <@NotNull R> void foo(R r);
    }

    public interface G1<T, E extends T, @NotNull X> {
        <R, @Nullable _A extends R> void foo(R r);
    }

    <R, @NotNull _A extends R, @Nullable K> void foo(R r) {

    }
}
