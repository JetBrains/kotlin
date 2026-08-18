// FULL_JDK

package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class InnerClassTypeAnnotation {

    public class Inner {
        public Inner(@Foo String foo) {
        }

        public @Foo String bar(String x, @Foo String y) { return null; }
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE_USE})
    public @interface Foo {}

}
