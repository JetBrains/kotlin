// FILE: RepeatableAnnotation.java

import java.lang.annotation.Repeatable

@Repeatable(RepeatableAnnotations.class)
public @interface RepeatableAnnotation {

}

// FILE: RepeatableAnnotations.java

public @interface RepeatableAnnotations {
     RepeatableAnnotation[] value();
}

// FILE: RepeatableUse.kt

// Error should be gone when Java 8 Target will be available
RepeatableAnnotation <!NON_SOURCE_REPEATED_ANNOTATION!>RepeatableAnnotation<!> class My

