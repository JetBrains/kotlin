// FIR_IDENTICAL
// SKIP_TXT

// FILE: ObjectStubSerializer.java
import org.jetbrains.annotations.*;

public interface ObjectStubSerializer<Y> {
    void indexStub(@NotNull Y stub);
}
// FILE: StubElement.java
public interface StubElement<T> {}
// FILE: IStubFileElementType.java
import org.jetbrains.annotations.*;

public class IStubFileElementType<X extends StubElement> extends ObjectStubSerializer<X> {
    @Override
    // IMO value parameter type should be T (may be I'm wrong)
    public void indexStub(@NotNull final StubElement stub) {}
}

// FILE: main.kt
class MakefileStubFileElementType : IStubFileElementType<StubElement<CharSequence>>() {
    // FIR: ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED: indexStub, should be ok
    // FE 1.0: Ok
}
