package test;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface A {
    String value() default "";
}

interface I1<T> {}
interface I2<T, K> {}
interface I3<T, K, L> {}

class A2<T, K> {}
class A3<T, K, L> {}

public class BaseClassTypeArguments<B> extends A3<@A B [][][][][], I1<I1<@A int @A [][]>>, A2<B, int [] [] @A []>> implements I1<@A Integer @A [][][]>, I2<@A B, B>, I3<@A B [][][][][], B, @A B> {
    class ImplementedInterfacesTypeArguments<B> implements I1<I2<I1<@A Integer @A [][][]>, I1<@A int [] @A []>>>, I2<@A B, B>, I3<@A B [][][][][], I1<I1<@A int @A [][]>>, I2<B, int [] [] @A []>> {
        public class BaseClassTypeArguments1<B> extends A3<@A B [][][][][], I1<I1<@A int @A [][]>>, A2<B, int [] [] @A []>> {

        }
    }
    static class BaseClassTypeArguments2<B> extends A3<@A B [][][][][], I1<I1<@A int @A [][]>>, A2<B, int [] [] @A []>> {

    }
}