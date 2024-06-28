// FIR_IDENTICAL
// FILE: main.kt

fun main() {
    MutableLong().toLong()
}

// FILE: MutableLong.java
public class MutableLong extends Number {

    private long value;

    public MutableLong() {
    }

    public int intValue() {
        return (int)this.value;
    }

    public long longValue() {
        return this.value;
    }

    public float floatValue() {
        return (float)this.value;
    }

    public double doubleValue() {
        return (double)this.value;
    }

    public Long toLong() {
        return this.longValue();
    }
}
