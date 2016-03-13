// FILE: J.java

public class J<T> {
    protected static String protectedFun() {
        return "OK";
    }
}

// FILE: 1.kt

class Derived : J<String>() {
    fun test(): String {
        return J.protectedFun()!!
    }
}

fun box(): String {
    return Derived().test()
}
