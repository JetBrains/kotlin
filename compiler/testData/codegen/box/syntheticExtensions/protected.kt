// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: JavaClass.java

public class JavaClass {
    protected String getOk() { return "OK"; }
}

// MODULE: main(lib)
// FILE: 1.kt

package p

import JavaClass

fun box(): String {
    return KotlinClass().ok()
}

class KotlinClass : JavaClass() {
    fun ok() = ok
}
