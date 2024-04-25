// LANGUAGE: +TypeEnhancementImprovementsInStrictMode

package test;

import org.jetbrains.annotations.*;

public class ClassTypeParameterBounds {
    interface I1 {}
    interface I2<T> {}
    interface I3<T> {}
    interface I4<T> {}

    interface G1<T extends @NotNull Object> { }
    class G2<_A, B extends @Nullable Integer> { }
    interface G3<_A, B extends Object & @NotNull I1> { }
    class G4<_A extends @NotNull B, B> { }
    interface G5<_A, B extends @Nullable _A> { }
    class G6<_A extends @Nullable I1, B, C, D extends @NotNull E, E, F> { }
    interface G7<_A extends Object & I2<@NotNull Integer> & @NotNull I3<String>> { }
    interface G8<_A extends Object & I2<? super @NotNull Integer> & @Nullable I3<? extends String>> { }

    interface G9<_A extends I4<Integer @NotNull []> & I2<? extends @NotNull Integer @NotNull []> & @NotNull I3<? extends Integer @NotNull []>> { }
    interface G10<_A extends I4<int @NotNull []> & I2<? extends @Nullable int @Nullable []> & @NotNull I3<? extends int @NotNull []>> { }
    interface G11<_A extends I4<Integer [] [] @NotNull []> & I2<? extends @NotNull Integer @NotNull [] [] [] []> & @NotNull I3<? extends Integer [] @NotNull []>> { }
    interface G12<_A extends I4<int @Nullable [][]> & I2<? extends @Nullable int [] [] @NotNull []> & @NotNull I3<? extends int []@NotNull [] []>> { }

    // class G13<_A extends Object, B extends I3<@NotNull _A> & @NotNull I2<_A>> { }
}
