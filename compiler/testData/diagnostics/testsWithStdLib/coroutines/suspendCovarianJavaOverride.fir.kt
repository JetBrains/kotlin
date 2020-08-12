// COMMON_COROUTINES_TEST
// FILE: I.kt

interface I {
    suspend fun foo(x: Int): String
}

// FILE: JavaClass.java
public class JavaClass implements I {
    @Override
    public String foo(int x, COROUTINES_PACKAGE.Continuation<String> continuation) {
        return null;
    }
}

// FILE: main.kt

import COROUTINES_PACKAGE.Continuation
class K1 : JavaClass()

class K2 : JavaClass() {
    override suspend fun foo(x: Int): String = ""
}

class K3 : JavaClass() {
    override fun foo(x: Int, y: Continuation<String>): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Any?<!> = null
}

fun builder(block: suspend () -> Unit) {}

fun main(x: Continuation<String>) {
    JavaClass().foo(5, x)
    K1().foo(6, x)
    K2().foo(7, x)
    K3().foo(8, x)

    builder {
        JavaClass().foo(1)
        K1().foo(2)
        K2().foo(3)
        K3().foo(4)

        JavaClass().foo(5, x)
        K1().foo(6, x)
        K2().foo(7, x)
        K3().foo(8, x)
    }
}
