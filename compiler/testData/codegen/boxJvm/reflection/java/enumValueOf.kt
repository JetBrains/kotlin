// TARGET_BACKEND: JVM
// WITH_REFLECT
// MODULE: lib1
// FILE: lib1/WithoutParameters.java
package lib1;

public enum WithoutParameters {
    X, Y, Z;
}

// MODULE: lib2
// JAVAC_OPTIONS: -parameters
// FILE: lib2/WithParameters.java
package lib2;

public enum WithParameters {
    A, B, C;
}

// MODULE: main(lib1, lib2)
// FILE: K.kt
import lib1.WithoutParameters
import lib2.WithParameters

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("value", WithoutParameters::valueOf.parameters.single().name)

    // javac generates the name "name" for this parameter, but we ignore it both in the compiler and kotlin-reflect.
    assertEquals("value", WithParameters::valueOf.parameters.single().name)

    return "OK"
}
