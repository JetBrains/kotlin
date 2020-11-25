// JAVAC_EXPECTED_FILE

package test;

import java.lang.annotation.*;

public class ClassTypeParameterUpperBounds {
    @Target(ElementType.TYPE_USE)
    @interface Anno {
        String value() default "";
    }

    interface I1 {}
    interface I2<T> {}
    interface I3<T> {}

    interface G1<T extends @Anno Object> { }
    class G2<A, B extends @Anno Integer> { }
    interface G3<A, B extends Object & @Anno I1> { }
    class G4<A extends @Anno B, B> { }
    interface G5<A, B extends @Anno A> { }
    class G6<A extends @Anno I1, B, C, D extends @Anno E, E, F> { }
    interface G7<A extends Object & I2<@Anno Integer> & @Anno I3<String>> { }
    //class G8<A extends Object, B extends I3<@Anno A> & @Anno I2<A>> { }
    // TODO: class My extends Foo<@Nullable String> {}
}
