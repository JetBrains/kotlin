// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: Owner.java

public class Owner {
    public static final String name = "Owner";
}

// FILE: Use.kt

val x = Owner.name
