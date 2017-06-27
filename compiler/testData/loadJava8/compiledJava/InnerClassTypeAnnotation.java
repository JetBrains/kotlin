// JAVAC_EXPECTED_FILE
package test;

// SKIP_IN_RUNTIME_TEST

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class InnerClassTypeAnnotation {

    public class Inner {
        public Inner(@Foo String foo) {
        }
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE_USE})
    public @interface Foo {}

}
