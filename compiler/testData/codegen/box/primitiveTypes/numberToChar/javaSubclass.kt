// TARGET_BACKEND: JVM_IR
// ISSUE: KT-23447
// WITH_STDLIB

// FILE: MyNumber.java

public class MyNumber extends Number {
    private final int value;

    public MyNumber(int value) {
        this.value = value;
    }

    @Override
    public int intValue() { return value; }

    @Override
    public long longValue() { return 0; }

    @Override
    public float floatValue() { return 0; }

    @Override
    public double doubleValue() { return 0; }
}

// FILE: box.kt

fun box(): String {
    val x = MyNumber('*'.code).toChar()
    if (x != '*') return "Fail 1: $x"

    val y = java.lang.Integer('+'.code).toChar()
    if (y != '+') return "Fail 2: $y"

    return "OK"
}
