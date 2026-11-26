// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: CompanionBlocks
// LANGUAGE_FEATURE_TOGGLED: CompanionExtensions

// FILE: J.java

public class J<T> {
    public interface I {
        void foo();
    }
}

// FILE: main.kt

class K<T> {
    fun interface I {
        fun foo()
    }

    typealias T = I
    typealias JT = J.I
}

fun test() {
    K::I
    J::<!JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE!>I<!>
    K::T
    K::<!JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE!>JT<!>

    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>K<Any><!>::I
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>J<Any><!>::<!JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE!>I<!>
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>K<Any><!>::T
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>K<Any><!>::<!JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE!>JT<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funInterface, functionDeclaration, interfaceDeclaration,
nestedClass, nullableType, typeParameter */
