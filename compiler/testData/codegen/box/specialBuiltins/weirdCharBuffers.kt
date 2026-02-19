// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// ^KT-63431 CharBufferXAllInherited.create().get(0) is resolved incorrectly in K1

// FILE: X.java
public interface X {
    char get(int index);
}

// FILE: CharBuffer.java
public abstract class CharBuffer implements CharSequence {
    public static CharBuffer create() {
        return new CharBuffer() {
            @Override public char charAt(int index) { return 'c'; }
            @Override public char get(int index) { return 'g'; }
            @Override public CharSequence subSequence(int start, int end) { return null; }
            @Override public int length() { return 0; }
        };
    }
    public abstract char charAt(int index);
    public abstract char get(int index);
}

// FILE: CharBufferX.java
public abstract class CharBufferX implements CharSequence, X {
    public static CharBufferX create() {
        return new CharBufferX() {
            @Override public char charAt(int index) { return 'c'; }
            @Override public char get(int index) { return 'g'; }
            @Override public CharSequence subSequence(int start, int end) { return null; }
            @Override public int length() { return 0; }
        };
    }
    public abstract char charAt(int index);
    public abstract char get(int index);
}

// FILE: NonCharSequenceBuffer.java
public abstract class NonCharSequenceBuffer implements X {
    public static NonCharSequenceBuffer create() {
        return new NonCharSequenceBuffer() {
            @Override public char charAt(int index) { return 'c'; }
            @Override public char get(int index) { return 'g'; }
        };
    }
    public abstract char charAt(int index);
    public abstract char get(int index);
}

// FILE: CharBufferCharAtInherited.java
public abstract class CharBufferCharAtInherited implements CharSequence {
    public static CharBufferCharAtInherited create() {
        return new CharBufferCharAtInherited() {
            @Override public char charAt(int index) { return 'c'; }
            @Override public char get(int index) { return 'g'; }
            @Override public CharSequence subSequence(int start, int end) { return null; }
            @Override public int length() { return 0; }
        };
    }
    public abstract char get(int index);
}

// FILE: CharBufferXAllInherited.java
public abstract class CharBufferXAllInherited implements CharSequence, X {
    public static CharBufferXAllInherited create() {
        return new CharBufferXAllInherited() {
            @Override public char charAt(int index) { return 'c'; }
            @Override public char get(int index) { return 'g'; }
            @Override public CharSequence subSequence(int start, int end) { return null; }
            @Override public int length() { return 0; }
        };
    }
}

// FILE: Y.java
public abstract class Y {
    protected abstract char get(int index);
}

// FILE: CharBufferXYAllInherited.java
public abstract class CharBufferXYAllInherited extends Y implements CharSequence, X {
    public static CharBufferXYAllInherited create() {
        return new CharBufferXYAllInherited() {
            @Override public char charAt(int index) { return 'c'; }
            @Override public char get(int index) { return 'g'; }
            @Override public CharSequence subSequence(int start, int end) { return null; }
            @Override public int length() { return 0; }
        };
    }
}

// FILE: CharAt.java
public interface CharAt {
    char charAt(int index);
}

// FILE: CharBufferXYCharAt.java
public abstract class CharBufferXYCharAt extends Y implements CharSequence, X, CharAt {
    public static CharBufferXYCharAt create() {
        return new CharBufferXYCharAt() {
            @Override public char charAt(int index) { return 'c'; }
            @Override public char get(int index) { return 'g'; }
            @Override public CharSequence subSequence(int start, int end) { return null; }
            @Override public int length() { return 0; }
        };
    }
}

// FILE: box.kt

fun box(): String {
    if (CharBuffer.create().get(0) != 'g') return "FAIL 1"
    if ((CharBuffer.create() as CharSequence).get(0) != 'c') return "FAIL 2"

    if (CharBufferX.create().get(0) != 'g') return "FAIL 3"
    if ((CharBufferX.create() as CharSequence).get(0) != 'c') return "FAIL 4"
    if ((CharBufferX.create() as X).get(0) != 'g') return "FAIL 5"

    if (NonCharSequenceBuffer.create().charAt(0) != 'c') return "FAIL 6"
    if (NonCharSequenceBuffer.create().get(0) != 'g') return "FAIL 7"
    if ((NonCharSequenceBuffer.create() as X).get(0) != 'g') return "FAIL 8"

    if (CharBufferCharAtInherited.create().get(0) != 'c') return "FAIL 9"
    if ((CharBufferCharAtInherited.create() as CharSequence).get(0) != 'c') return "FAIL 10"

    if (CharBufferXAllInherited.create().get(0) != 'g') return "FAIL 11"
    if ((CharBufferXAllInherited.create() as CharSequence).get(0) != 'c') return "FAIL 12"
    if ((CharBufferXAllInherited.create() as X).get(0) != 'g') return "FAIL 13"

    if (CharBufferXYAllInherited.create().get(0) != 'g') return "FAIL 14"
    if ((CharBufferXYAllInherited.create() as CharSequence).get(0) != 'c') return "FAIL 15"
    if ((CharBufferXYAllInherited.create() as X).get(0) != 'g') return "FAIL 16"
    if ((CharBufferXYAllInherited.create() as Y).get(0) != 'g') return "FAIL 17"

    if (CharBufferXYCharAt.create().get(0) != 'g') return "FAIL 18"
    if ((CharBufferXYCharAt.create() as CharSequence).get(0) != 'c') return "FAIL 19"
    if ((CharBufferXYCharAt.create() as X).get(0) != 'g') return "FAIL 20"
    if ((CharBufferXYCharAt.create() as Y).get(0) != 'g') return "FAIL 21"
    if ((CharBufferXYCharAt.create() as CharAt).charAt(0) != 'c') return "FAIL 22"

    return "OK"
}
