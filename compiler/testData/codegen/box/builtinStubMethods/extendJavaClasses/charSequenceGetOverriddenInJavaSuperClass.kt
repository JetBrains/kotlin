// TARGET_BACKEND: JVM
// FULL_JDK

// FILE: charSequenceGetOverriddenInJavaSuperClass.kt
abstract class KACharSequence : JACharSequence()

class Test(s: String) : JCharSequence(s)

fun box(): String {
    val t = Test("OK")
    return "" + t[0] + t[1]
}

// FILE: JACharSequence.java
public abstract class JACharSequence implements CharSequence {
    @Override
    public char charAt(int index) {
        return myCharAt(index);
    }

    protected abstract char myCharAt(int index);
}

// FILE: JCharSequence.java
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

public class JCharSequence extends KACharSequence {
    private final CharSequence d;

    public JCharSequence(CharSequence d) {
        this.d = d;
    }

    @Override
    public char myCharAt(int index) {
        return d.charAt(index);
    }

    public int getLength() {
        return d.length();
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return d.subSequence(start, end);
    }

    @Override
    public String toString() {
        return d.toString();
    }

    @NotNull
    @Override
    public IntStream codePoints() {
        return d.codePoints();
    }
}