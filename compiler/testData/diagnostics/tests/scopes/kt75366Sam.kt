// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-75366
// IGNORE_REVERSED_RESOLVE: KT-75366
// FILE: K.kt
interface K<T> {
    fun foo(t: T)
}

// FILE: J.java
public interface J extends K<String> {
    static J create() {
        return null;
    }

    static J create(String str) {
        return null;
    }

    static void take(J j) {}
}

// FILE: main.kt
fun main() {
    J.create()
    J.take { }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, interfaceDeclaration, javaFunction, javaType, lambdaLiteral,
nullableType, samConversion, typeParameter */
