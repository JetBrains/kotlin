// LL_FIR_DIVERGENCE
// LL runners don't run the backend, so they can't show `CONFLICTING_INHERITED_JVM_DECLARATIONS`.
// LL_FIR_DIVERGENCE
// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-54866

// FILE: JavaEmptyAnno.java
public @interface JavaEmptyAnno {}

// FILE: JavaNonEmptyAnno.java
public @interface JavaNonEmptyAnno {
    String getQuery();
}

// FILE: JavaEmptyAnnoClass.java
import java.lang.annotation.Annotation;

public class JavaEmptyAnnoClass implements JavaEmptyAnno {
    @Override
    public Class<? extends Annotation> annotationType() { return getClass(); }
}

// FILE: JavaNonEmptyAnnoClass.java
import java.lang.annotation.Annotation;

public class JavaNonEmptyAnnoClass implements JavaNonEmptyAnno {
    @Override
    public Class<? extends Annotation> annotationType() { return getClass(); }

    @Override
    public String getQuery() { return "query"; }
}

// FILE: KotlinAnnoClasses.kt
class KotlinEmptyAnnoClass : JavaEmptyAnnoClass()

class KotlinNonEmptyAnnoClass : JavaNonEmptyAnnoClass()

fun main() {
    KotlinEmptyAnnoClass()
    KotlinNonEmptyAnnoClass().query
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaProperty, javaType */
