// FILE: J.java

public class J {
    protected final String protectedProperty;

    public J(String str) {
        protectedProperty = str;
    }

    protected static String protectedFun() {
        return "OK";
    }
}

// FILE: 1.kt

class A : J(J.protectedFun()) {
    fun test(): String {
        return protectedProperty!!
    }
}

fun box(): String {
    return A().test()
}
