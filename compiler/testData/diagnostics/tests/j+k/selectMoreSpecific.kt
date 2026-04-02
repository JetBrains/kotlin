// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// FILE: A.java
public class A {
    public String foo() {}
    public CharSequence foo() {}

    public static String bar() {}
    public static CharSequence bar() {}
}

// FILE: main.kt

fun foo(a: A) {
    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>() <!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><<!CANNOT_INFER_PARAMETER_TYPE!>String<!>>() }
    A.<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>() <!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><<!CANNOT_INFER_PARAMETER_TYPE!>String<!>>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix, javaType,
lambdaLiteral, nullableType, typeParameter, typeWithExtension */
