// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ReferencesToSyntheticJavaProperties

// FILE: AnnInterface.java
public @interface AnnInterface {
    public String javaMethod() default "";
}

// FILE: Main.kt
val prop = AnnInterface::javaMethod

/* GENERATED_FIR_TAGS: javaCallableReference, javaType, propertyDeclaration */
