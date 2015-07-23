// SKIP_IN_RUNTIME_TEST

package test;

import java.lang.annotation.*;

public class AnnotationRetentions {

    public @interface BaseAnnotation {

    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SourceAnnotation {

    }

    @Retention(RetentionPolicy.CLASS)
    public @interface BinaryAnnotation {

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface RuntimeAnnotation {

    }

    @BaseAnnotation class BaseClass {

    }

    @SourceAnnotation class SourceClass {

    }

    @BinaryAnnotation class BinaryClass {

    }

    @RuntimeAnnotation class RuntimeClass {

    }
}