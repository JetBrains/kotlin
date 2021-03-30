// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: protectedPack/J.java

package protectedPack;

public class J {
    protected String foo() {
        return "fail";
    }
}

// MODULE: main(lib)
// FILE: 1.kt

package protectedPackKotlin

import protectedPack.J

class Derived : J() {
    protected override fun foo(): String? {
        return "OK"
    }

    fun test(): String {
        return foo()!!
    }
}

fun box(): String {
   return Derived().test()
}
