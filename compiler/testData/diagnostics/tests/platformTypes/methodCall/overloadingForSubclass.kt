// FILE: p/Super.java
package p;

public interface Super {}

// FILE: p/Sub.java
package p;

public interface Sub extends Super {}

// FILE: p/Util.java

package p;

public abstract class Util {
    public abstract void foo(String s, Super sup)
    public void foo(String s, Sub sub) {}
}

// FILE: k.kt

import p.*

class C: Util() {
    override fun foo(s: String, sub: Super) {}
}

fun foo(sub: Sub) {
    C().foo("", sub)
}