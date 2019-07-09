// SKIP_IN_RUNTIME_TEST
package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


class AnnotatedTypeInFun {

    @Target(ElementType.TYPE_USE)
    public @interface Anno {
        String value();
    }

    void foo(@Anno("a") String a, @Anno("b") String b) {
    }
}