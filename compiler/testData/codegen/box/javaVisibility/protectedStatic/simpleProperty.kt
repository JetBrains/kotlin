// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: Base.java

public class Base {
    protected static final String protectedProperty = "OK";
}

// MODULE: main(lib)
// FILE: 1.kt

class Derived : Base() {
    fun test(): String {
        return Base.protectedProperty!!
    }
}

fun box(): String {
    return Derived().test()
}
