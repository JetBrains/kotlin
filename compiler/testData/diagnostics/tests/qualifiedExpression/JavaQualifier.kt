// RUN_PIPELINE_TILL: BACKEND
// FILE: Owner.java

public class Owner {
    public static final String name = "Owner";
}

// FILE: Use.kt

val x = Owner.name

/* GENERATED_FIR_TAGS: javaProperty, propertyDeclaration */
