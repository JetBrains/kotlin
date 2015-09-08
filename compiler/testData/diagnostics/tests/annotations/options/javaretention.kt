// FILE: AnnotationRetentions.java

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
}

// FILE: AnnotationRetentions.kt

@AnnotationRetentions.BaseAnnotation class BaseClass

@AnnotationRetentions.SourceAnnotation class SourceClass

@AnnotationRetentions.BinaryAnnotation class BinaryClass

@AnnotationRetentions.RuntimeAnnotation class RuntimeClass
