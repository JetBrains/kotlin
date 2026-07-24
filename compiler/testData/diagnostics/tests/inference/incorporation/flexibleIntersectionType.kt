// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// FILE: J.java
public class J {
    public static <T> T makeFlexible(T t) {
        return t;
    }
}

// FILE: test.kt
val d: Double? = null
val l: Long? = null
val x = J.makeFlexible(if (true) d else l)

/* GENERATED_FIR_TAGS: flexibleType, ifExpression, intersectionType, javaFunction, propertyDeclaration */
