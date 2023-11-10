// FIR_IDENTICAL
// SCOPE_DUMP: CharBuffer:get;charAt, CharBufferX:get;charAt, NonCharSequenceBuffer:get;charAt, NonCharBuffer:get;charAt, CharBufferXAllInherited:get;charAt, CharBufferXYAllInherited:get;charAt, CharBufferXYCharAt:get;charAt

// FILE: X.java
public interface X {
    char get(int index);
}

// FILE: CharBuffer.java
public abstract class CharBuffer implements CharSequence {
    // Should override 'kotlin.CharSequence.get'
    public final char charAt(int index) {
        return get(position() + checkIndex(index, 1));
    }

    // The key problem here is that `get` has the same signature as kotlin.CharSequence.get but completely different semantics
    // Should override nothing
    public abstract char get(int index);
}

// FILE: CharBufferX.java
public abstract class CharBufferX implements CharSequence, X {
    // Should override 'kotlin.CharSequence.get'
    public final char charAt(int index) {
        return get(position() + checkIndex(index, 1));
    }

    // Should override X.get
    public abstract char get(int index);
}

// FILE: NonCharSequenceBuffer.java
public abstract class NonCharSequenceBuffer implements X {
    // Should override nothing and be available as 'charAt', not as 'get'
    public final char charAt(int index) {
        return get(position() + checkIndex(index, 1));
    }

    // Should override 'X.get'
    public abstract char get(int index);
}

// FILE: NonCharBuffer.java
public abstract class NonCharBuffer implements CharSequence {
    // Not overriding charAt explicitly but inherited through kotlin.CharSequence.get

    // Should override nothing
    public abstract char get(int index);
}

// FILE: CharBufferXAllInherited.java
public abstract class CharBufferXAllInherited implements CharSequence, X {
}

// FILE: Y.java
public class Y {
    protected abstract char get(int index);
}

// FILE: CharBufferXYAllInherited.java
public abstract class CharBufferXYAllInherited extends Y implements CharSequence, X {
}

// FILE: CharAt.java
public interface CharAt {
    char charAt(int index);
}

// FILE: CharBufferXYCharAt.java
public class CharBufferXYCharAt extends Y implements CharSequence, X, CharAt {
}

// FILE: main.kt

fun test(
    charBuffer: CharBuffer,
    charBufferX: CharBufferX,
    nonCharSequenceBuffer: NonCharSequenceBuffer,
    nonCharBuffer: NonCharBuffer,
    charBufferXAllInherited: CharBufferXAllInherited,
    charBufferXYAllInherited: CharBufferXYAllInherited,
    charBufferXYCharAt: CharBufferXYCharAt,
) {
    charBuffer.get(0)
    (charBuffer as CharSequence).get(0)

    charBufferX.get(0)
    (charBufferX as CharSequence).get(0)

    nonCharSequenceBuffer.get(0)
    nonCharSequenceBuffer.charAt(0)

    nonCharBuffer.get(0)
    (nonCharBuffer as CharSequence).get(0)

    charBufferXAllInherited.get(0)
    (charBufferXAllInherited as CharSequence).get(0)

    charBufferXYAllInherited.get(0)
    (charBufferXYAllInherited as CharSequence).get(0)

    charBufferXYCharAt.get(0)
    (charBufferXYCharAt as CharSequence).get(0)
    (charBufferXYCharAt as CharAt).charAt(0)
}
