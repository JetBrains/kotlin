// FILE: protectedPack/J.java

package protectedPack;

public class J {
    protected String foo() {
        return "OK";
    }
}

// FILE: 1.kt

package protectedPack

fun box(): String {
    return J().foo()!!
}
