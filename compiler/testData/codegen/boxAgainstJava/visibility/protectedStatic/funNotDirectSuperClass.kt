// FILE: J.java

public class J {
    protected static String protectedFun() {
        return "OK";
    }
}

// FILE: 1.kt

open class A : J() {}

class Derived : A() {
    fun test(): String {
        return J.protectedFun()!!
    }
}

fun box(): String {
    return Derived().test()
}
