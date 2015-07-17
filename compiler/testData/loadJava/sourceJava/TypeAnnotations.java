package test;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface A {
    String value() default "";
}

interface G<T> {}
interface G2<A, B> {}

public interface TypeAnnotations<TT> {
    void f(G<@A String> p);
    void f(G2<@A String, @A Integer> p);
}