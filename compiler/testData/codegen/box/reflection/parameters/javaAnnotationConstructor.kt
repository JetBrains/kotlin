// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public @interface J {
    short s();
    long j();
    boolean z();
    int i();
    float f();
    char c();
    double d();
}

// FILE: K.kt

import kotlin.reflect.KParameter
import kotlin.test.assertEquals

fun box(): String {
    val ctor = J::class.constructors.single()

    // We sort parameters by name for consistency
    assertEquals(listOf("c", "d", "f", "i", "j", "s", "z"), ctor.parameters.map { it.name })
    assert(ctor.parameters.all { it.kind == KParameter.Kind.VALUE })

    return "OK"
}
