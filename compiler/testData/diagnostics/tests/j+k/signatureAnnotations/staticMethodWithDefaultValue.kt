// IGNORE_BACKEND: JS, NATIVE

// FILE: A.java
// ANDROID_ANNOTATIONS

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
