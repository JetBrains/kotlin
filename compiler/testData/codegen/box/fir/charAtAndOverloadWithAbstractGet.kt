// TARGET_BACKEND: JVM_IR
// ISSUE: KT-66463

// FILE: A.java
public abstract class A implements CharSequence {
    public final int length() {
        return 0;
    }

    public final char charAt(int index) {
        return 'K';
    }
    public final CharSequence subSequence(int startIndex, int endIndex) {
        return this;
    }

    public abstract char get(int index);
}

// FILE: B.kt
class B : A() {
    override fun get(index: Int) = 'A'
}

fun box(): String {
    val b = B()
    if (b.get(0) != 'A') return "FAIL"
    return "OK"
}
