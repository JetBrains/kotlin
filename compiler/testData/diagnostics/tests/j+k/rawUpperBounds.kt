// FIR_IDENTICAL
// SKIP_TXT

// FILE: StubElement.java
public interface StubElement<T extends CharSequence> {}

// FILE: IStubFileElementType.java
import org.jetbrains.annotations.*;

public class IStubFileElementType<X extends StubElement> {
    public X getFoo() { return null; }
}

// FILE: main.kt
fun foo(i: IStubFileElementType<*>) {
    bar(i.getFoo())
}

fun bar(w: StubElement<CharSequence>) {}
