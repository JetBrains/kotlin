// FILE: JavaEnum.java
public enum JavaEnum {
    A, B, C;

    public String getName() {
        return "FromJava";
    }
}

// FILE: Usage.kt
fun foo() {
    JavaEnum.A.na<caret>me
}

// IGNORE_STABILITY_K2: candidates
// ^KT-69962