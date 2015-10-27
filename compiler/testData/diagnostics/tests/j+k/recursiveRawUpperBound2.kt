// FILE: XYZ.java
public interface XYZ<X extends X> {
    XYZ foo() {}
}

// FILE: main.kt

fun main(xyz: XYZ<*>) = xyz.foo()