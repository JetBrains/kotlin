package test;

import java.lang.annotation.*;
public class TypeAnnotations {
    @Target(ElementType.TYPE_USE)
    @interface A {
        String value() default "";
    }

    interface G<T> {
    }

    interface G2<A, B> {
    }

    public interface MyClass<TT> {
        void f(G<@A String> p);

        void f(G2<@A String, @A("abc") Integer> p);
    }
}
