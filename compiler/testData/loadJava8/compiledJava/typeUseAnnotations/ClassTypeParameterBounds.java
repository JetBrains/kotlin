// JAVAC_EXPECTED_FILE

package test;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface A {
    String value() default "";
}

public class ClassTypeParameterBounds {
    interface I1 {}
    interface I2<T> {}
    interface I3<T> {}
    interface I4<T> {}

    interface G1<T extends @A Object> { }
    class G2<_A, B extends @A Integer> { }
    interface G3<_A, B extends Object & @A I1> { }
    class G4<_A extends @A B, B> { }
    interface G5<_A, B extends @A _A> { }
    class G6<_A extends @A I1, B, C, D extends @A E, E, F> { }
    interface G7<_A extends Object & I2<@A Integer> & @A I3<String>> { }
    interface G8<_A extends Object & I2<? super @A Integer> & @A I3<? extends String>> { }

    interface G9<_A extends I4<Integer @A []> & I2<? extends @A Integer @A []> & @A I3<? extends Integer @A []>> { }
    interface G10<_A extends I4<int @A []> & I2<? extends @A int @A []> & @A I3<? extends int @A []>> { }
    interface G11<_A extends I4<Integer [] [] @A []> & I2<? extends @A Integer @A [] [] [] []> & @A I3<? extends Integer [] @A []>> { }
    interface G12<_A extends I4<int @A [][]> & I2<? extends @A int [] [] @A []> & @A I3<? extends int []@A [] []>> { }

    // class G13<_A extends Object, B extends I3<@A _A> & @A I2<_A>> { }
}
