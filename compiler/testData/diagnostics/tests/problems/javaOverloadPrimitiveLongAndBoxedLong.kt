// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-12619

// FILE: MyClass.java

public class MyClass {
    public long getLong(String key, long defaultValue) {
        return defaultValue;
    }
    public Long getLong(String key, Long defaultValue) {
        return defaultValue;
    }
}

// FILE: main.kt

fun test(c: MyClass) {
    c.getLong("key", 10000L)
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
