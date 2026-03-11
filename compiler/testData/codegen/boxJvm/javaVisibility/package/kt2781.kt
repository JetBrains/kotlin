// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: J.java

import java.lang.String;

class J {
    String value;

    J(String value) {
        this.value = value;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box() = J("OK").value
