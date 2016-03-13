// FILE: Base.java

public class Base {
    protected static String protectedFun() {
        return "OK";
    }
}

// FILE: 1.kt

class Derived : Base() {
    fun test(): String {
        return Base.protectedFun()!!
    }
}

fun box(): String {
    return Derived().test()
}
