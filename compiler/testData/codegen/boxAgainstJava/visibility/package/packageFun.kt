// FILE: protectedPack/J.java

package protectedPack;

public class J {
    String test() {
        return "OK";
    }
}

// FILE: 1.kt

package protectedPack

fun box(): String {
    return J().test()!!
}
