// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82021
// FIR_DUMP
// WITH_STDLIB

// FILE: JavaClass.java

import java.util.*;

public class JavaClass {
    public static <T> Collection<T> filter(List<? extends T> arg) { return arg; }
}

// FILE: Test.kt
fun test(list: MutableList<out String?>) {
    // T is inferred here to CapturedType, but then approximated to String?
    // The test can be made green by precise actions for subType.isCapturedType() in simplifyLowerConstraints
    // (see the experimental feature PreciseSimplificationToFlexibleLowerConstraint)
    // However, such a change will affect resolve of resImplicit.get(0).toString(), as it's String? VS String! situation
    val res: Collection<String> <!INITIALIZER_TYPE_MISMATCH("Collection<String>; (Mutable)Collection<String?>!")!>=<!> JavaClass.filter(list)
    // typeof = (Mutable)Collection<String?>!
    val resImplicit = JavaClass.filter(list)
}

fun testNotNull(list: MutableList<out String>) {
    // T is inferred here to CapturedType, but then approximated to String!
    val res: Collection<String> = JavaClass.filter(list)
    // typeof = (Mutable)Collection<String!>!
    val resImplicit = JavaClass.filter(list)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, localProperty, nullableType, outProjection,
propertyDeclaration */
