// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: A.java

class A {
    public A() {}

    public A(String x) {}

    public A(long l, double z) {}
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    A()
    A("")
    A(0.toLong(), 0.0)
    return "OK"
}
