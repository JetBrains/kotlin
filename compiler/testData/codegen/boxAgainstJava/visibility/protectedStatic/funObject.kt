// FILE: J.java

public class J {
    protected static String protectedFun() {
        return "OK";
    }
}

// FILE: 1.kt

object A : J() {
    fun test(): String {
        return J.protectedFun()!!
    }
}

fun box(): String {
    return A.test()
}
