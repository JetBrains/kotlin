// FILE: Base.java

public class Base {
    protected static class Inner {
        public Inner() {}
        public String foo() {
            return "OK";
        }
    }
}

// FILE: 1.kt

class Derived : Base() {
    fun test(): String {
        return Base.Inner().foo()!!
    }
}

fun box(): String {
    return Derived().test()
}
