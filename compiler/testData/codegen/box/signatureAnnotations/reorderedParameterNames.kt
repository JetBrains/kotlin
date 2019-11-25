// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

public class A {
    public int connect(@ParameterName("host") int host, @ParameterName("port") int port) {
        return host;
    }
}

// FILE: test.kt
fun box(): String {
    val test = A()

    if (test.connect(host = 42, port = 8080) != 42)  {
        return "FAIL 1"
    }

    if (test.connect(port = 1234, host = 5678) != 5678) {
        return "FAIL 2"
    }

    if (test.connect(9876, 4321) != 9876)  {
        return "FAIL 3"
    }

    return "OK"
}