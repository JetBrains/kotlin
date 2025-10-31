// FIR_DUMP
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81988

// FILE: A.java
public class A<T> {
    public static <F> F foo(java.util.List<? extends F> l) { return null; }
    public static <F> A<F> bar(java.util.List<? extends F> l) { return null; }
    public T getValue() { return null; }
}

// FILE: main.kt

val String?.length: String get() = ""

fun box(x: List<String?>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String..kotlin.String?)")!>A.foo(x)<!>
    A.foo(x).length.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>length<!>
    A.foo(x).length.times(1)
    A.foo(x)?.length?.times(1)

    <!DEBUG_INFO_EXPRESSION_TYPE("(A<(kotlin.String..kotlin.String?)>..A<(kotlin.String..kotlin.String?)>?)")!>A.bar(x)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String..kotlin.String?)")!>A.bar(x).getValue()<!>
    A.bar(x).getValue().length.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>length<!>
    A.bar(x).getValue().length.times(1)
    A.bar(x).getValue()?.length?.times(1)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, getter, integerLiteral, javaFunction, nullableType,
propertyDeclaration, propertyWithExtensionReceiver, safeCall, stringLiteral */
