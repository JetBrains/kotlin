// FIR_IDENTICAL
// IGNORE_BACKEND: JS, NATIVE

// ANDROID_ANNOTATIONS
// FILE: A.java

import kotlin.annotations.jvm.internal.*;

class A {
    public static String withDefault(@DefaultValue("OK") String arg) {
        return arg;
    }
}

// FILE: test.kt
fun box(): String {
    return A.withDefault();
}
