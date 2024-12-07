// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: J1
// FILE: javaConstructorWithTypeParameters.kt

fun test1() = J1<Int>()

fun test2() = J1<Int, Int>(1)

fun test3(j1: J1<Any>) = j1.J2<Int>()

fun test4(j1: J1<Any>) = j1.J2<Int, Int>(1)

// FILE: J1.java
public class J1<T1> {
    public J1() {}
    public <X1> J1(X1 x1) {}

    public class J2<T2> {
        public J2() {}
        public <X2> J2(X2 x2) {}
    }
}