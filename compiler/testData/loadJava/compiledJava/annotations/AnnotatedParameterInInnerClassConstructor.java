// SKIP_IN_FIR_TEST
// SKIP_IN_RUNTIME_TEST
package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

class AnnotatedParameterInInnerClassConstructor {

    @Target(ElementType.TYPE_USE)
    public @interface Anno {
    }

    class InnerGeneric<T, K> {
    }

    Integer foo(InnerGeneric<@Anno String, InnerGeneric<String, @Anno InnerGeneric<@Anno String, String>>> a) { return 11; }
}