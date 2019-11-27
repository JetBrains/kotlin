// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

import java.util.List;

public interface J {
    List foo();
}

// FILE: K.kt

import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(Any::class.java, J::foo.returnType.arguments.single().type!!.javaType)

    return "OK"
}
