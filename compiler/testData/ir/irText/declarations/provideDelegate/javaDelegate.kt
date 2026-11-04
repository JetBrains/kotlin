// WITH_STDLIB
// IGNORE_BACKEND: JKLIB
// ^ KT-91301: JKlib can't mangle properties without accessors or backing field.
// TARGET_BACKEND: JVM
// SKIP_KT_DUMP

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
