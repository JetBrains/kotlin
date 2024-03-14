// TARGET_BACKEND: JVM_IR
// ISSUE: KT-66463
// JVM_TARGET: 1.8

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

// FILE: Proxy.java

public interface Proxy {
    default char get(int index) {
        return 'X';
    }
}

// FILE: B.kt
class B : A(), Proxy {
    override fun get(index: Int) = 'A'
}

fun box(): String {
    val b = B()
    if (b.get(0) != 'A') return "FAIL"
    return "OK"
}
