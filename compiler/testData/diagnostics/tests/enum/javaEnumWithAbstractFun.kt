// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// FILE: A.java
public enum A {

    ENTRY {
        public String s() {
            return "s";
        }
    };
    public abstract String s();
}

// FILE: test.kt
fun main() {
    checkSubtype<String?>(A.ENTRY.s())
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, javaFunction, javaProperty, javaType, nullableType, typeParameter, typeWithExtension */
