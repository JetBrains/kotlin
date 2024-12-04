// TARGET_BACKEND: JVM

// FILE: SubClass.kt

class SubClass: BaseClass() {
    inner class InnerClass {
        fun foo() = super@SubClass.foo()
    }
}

fun box() = SubClass().InnerClass().foo()

// FILE: BaseClass.java
public class BaseClass {
    String foo() {
        return "OK";
    }
}