// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: J1
// FILE: constructorWithOwnTypeParametersCall.kt

fun testKotlin() = K1<Int>().K2<String>()

fun testJava() = J1<Int, String>().J2<Double, CharSequence>()

class K1<T1 : Number> {
    inner class K2<T2 : CharSequence>
}

// FILE: J1.java
public class J1<X1 extends Number> {
    public <Y1 extends CharSequence> J1() {}

    public class J2<X2 extends Number> {
        public <Y2 extends CharSequence> J2() {}
    }
}
