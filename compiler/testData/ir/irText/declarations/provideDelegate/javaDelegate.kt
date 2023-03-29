// WITH_STDLIB
// TARGET_BACKEND: JVM
// SKIP_KT_DUMP

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57433

// FILE: box.kt

package k

import J

var p1 by J()::foo
var p2 by J()::foo

fun box(): String {
    p1 = "O"
    p2 = "K"
    return p1 + p2
}

// FILE: J.java

public class J {
    private String s = "Fail";

    public void setFoo(String s) {
        this.s = s;
    }

    public String getFoo() {
        return s;
    }
}
