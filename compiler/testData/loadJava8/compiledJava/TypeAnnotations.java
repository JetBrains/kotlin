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

    // Currently annotations on type parameters and arguments are not loaded from compiled code because of IDEA-153093
    // Once it will be fixed check if KT-11454 is ready to be resolved
    public interface MyClass<TT> {
        void f(G<@A String> p);

        void f(G2<@A String, @A("abc") Integer> p);
    }
}
