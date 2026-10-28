package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Outer {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ann {
        int value();
    }
}
