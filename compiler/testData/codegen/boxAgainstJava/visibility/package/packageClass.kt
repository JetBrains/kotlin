// FILE: protectedPack/J.java

package protectedPack;

class J {
    public String test() {
        return "OK";
    }
}

// FILE: 1.kt

package protectedPack

fun box(): String {
    return J().test()!!
}
