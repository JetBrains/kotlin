// FILE: protectedPack/J.java

package protectedPack;

public class J {
    protected static class Inner {
        public String foo() {
            return "OK";
        }
    }
}

// FILE: 1.kt

package protectedPack

class Derived : J() {
    fun test(): String {
        return J.Inner().foo()!!
    }
}

fun box(): String {
    return Derived().test()
}
