// FILE: Base.java

public class Base {
    public static class A {
        protected static class B {
            public B() {
            }

            public String foo() {
                return "OK";
            }
        }
    }
}

// FILE: 1.kt

class Derived : Base.A() {
    fun test(): String {
        return Base.A.B().foo()!!
    }
}

fun box(): String {
    return Derived().test()
}
