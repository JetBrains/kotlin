// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: CompanionBlocksAndExtensions

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
}

fun test() {
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>K<!>::I
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>J<!>::<!JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE!>I<!>

    K<Any>::I
    J<Any>::<!JAVA_SAM_INTERFACE_CONSTRUCTOR_REFERENCE!>I<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funInterface, functionDeclaration, interfaceDeclaration,
nestedClass, nullableType, typeParameter */
