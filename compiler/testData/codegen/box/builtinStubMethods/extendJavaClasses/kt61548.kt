// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS

// FILE: Function42.java

package abacaba.kotlin;

public abstract class Function42 implements CharSequence {
    @Override
    public char charAt(int index) {
        return 'a';
    }
}

// FILE: box.kt

package abacaba.kotlin

abstract class KACharSequence : Function42() {
    companion object {
        const val x  = "OK"
    }
}

fun box(): String {
    return KACharSequence.x
}