// !LANGUAGE: -ReferencesToSyntheticJavaProperties
// FIR_IDENTICAL

// FILE: AnnInterface.java
public @interface AnnInterface {
    public String javaMethod() default "";
}

// FILE: Main.kt
val prop = AnnInterface::javaMethod
