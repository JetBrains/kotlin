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

RepeatableAnnotation RepeatableAnnotation class My

