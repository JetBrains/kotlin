// SKIP_IN_RUNTIME_TEST
package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

class AnnotatedTypeInInnerClassConstructor {

    @Target(ElementType.TYPE_USE)
    public @interface Anno {
        String value();
    }

    class Inner {
        Inner(@Anno("a") String a , @Anno("b")  String b) {}
    }

    class InnerGeneric<T> {
        InnerGeneric(@Anno("a") String a , @Anno("b")  String b) {}
    }
}