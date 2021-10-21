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
    bar(<!ARGUMENT_TYPE_MISMATCH!>i.getFoo()<!>) // In FIR, `i.getFoo()` has a type ft<StubElement<*>, StubElement<*>?>, while in FE1.0 it's ft<StubElement<CharSequence>, StubElement<*>?>
}

fun bar(w: StubElement<CharSequence>) {}
