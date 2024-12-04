// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// MODULE: lib
// FILE: J.java
package bar;

import java.util.List;

public interface J {
    Object flexibleNullability();
    Object[] flexibleVariance();
    List<Object> flexibleMutability();
    List rawType();
}

// MODULE: main(lib)
// FILE: k.kt
package foo

import bar.J

class Kotlin {
    fun foo(j: J) = j.flexibleNullability()
    fun foo2(j: J) = j.flexibleVariance()
    fun foo3(j: J) = j.flexibleMutability()
    fun foo4(j: J) = j.rawType()
}
