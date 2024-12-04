// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: jjj
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

// MODULE: lib(jjj)
// FILE: lib.kt
package k

import J

var p1 by J()::foo
var p2 by J()::foo

// MODULE: main(lib)
// FILE: box.kt

import k.p1
import k.p2

fun box(): String {
    p1 = "O"
    p2 = "K"
    return p1 + p2
}

