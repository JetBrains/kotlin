// FILE: J.java

import java.lang.String;

class J {
    String value;

    J(String value) {
        this.value = value;
    }
}

// FILE: 1.kt

fun box() = J("OK").value
