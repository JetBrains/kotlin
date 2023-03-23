// FIR_IDENTICAL
// !LANGUAGE: +IntrinsicConstEvaluation

// FILE: CompressionType.java
public enum CompressionType {
    OK("NOT OK");

    public final String name;
    CompressionType(String name) {
        this.name = name;
    }
}

// FILE: main.kt
const val name = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>CompressionType.OK.name<!>
