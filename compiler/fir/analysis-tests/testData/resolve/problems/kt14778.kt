// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-14778

// FILE: Opcodes.java
public interface Opcodes {
    int V1_8 = 1;
}

// FILE: JDKVersion.java
public enum JDKVersion implements Opcodes {
    V1_8(Opcodes.V1_8);

    JDKVersion(int x) {}
    public static void run(JDKVersion JDKVersion) {}
}

// FILE: test.kt
// KT-14778: Unable to use Java enum entry when base class has field with same name

fun main(args: Array<String>) {
    JDKVersion.run(JDKVersion.V1_8)
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaProperty, javaType */
