// FILE: J.java

public class J {
    protected static String protectedFun() {
        return "OK";
    }
}

// FILE: 1.kt

class A {
    companion object : J() {
        fun test(): String {
            return J.protectedFun()!!
        }
    }
}

fun box(): String {
    return A.test()
}
