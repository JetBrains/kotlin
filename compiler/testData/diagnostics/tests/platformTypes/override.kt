// FILE: p/I.java

package p;

import p.J.Param;

public interface I {
    String s();
}

// FILE: p/J.java

package p;

public class J implements I {
    public String s() { return null; }
}

// FILE: k.kt
import p.*

fun test() {
    val s = J().s()
    s.get(0)
    s!!.get(0)
}
