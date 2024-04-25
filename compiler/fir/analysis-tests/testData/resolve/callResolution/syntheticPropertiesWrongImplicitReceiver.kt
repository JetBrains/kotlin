// CHECK_TYPE
// FILE: JavaClass.java
public class JavaClass {
    public String getFoo() {
        return "";
    }
}

// FILE: main.kt

interface A

val A.foo: Int get() = 1

fun bar(a: A) {
    a.foo checkType { _<Int>() } // OK, resolved to extension property
}

fun JavaClass.bar(a: A) {
    a.foo checkType { _<Int>() }
}
