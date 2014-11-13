package test;

import java.lang.annotation.RetentionPolicy;

public interface EnumInParam {
    public @interface MyRetention {
        RetentionPolicy value();
    }

    @MyRetention(RetentionPolicy.RUNTIME)
    public @interface RetentionAnnotation {
        String value();
    }
}
