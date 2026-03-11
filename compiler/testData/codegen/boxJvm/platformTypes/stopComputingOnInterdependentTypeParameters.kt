// TARGET_BACKEND: JVM
// FILE: AbstractMessageLite.java

public abstract class AbstractMessageLite<
        MessageType extends AbstractMessageLite<MessageType, BuilderType>,
BuilderType extends AbstractMessageLite.Builder<MessageType, BuilderType>>
{
    public void writeDelimitedTo() {}

    public abstract static class Builder<
        MessageType extends AbstractMessageLite<MessageType, BuilderType>,
    BuilderType extends Builder<MessageType, BuilderType>>
            {}
}

// FILE: H.java
public class H extends AbstractMessageLite {}

// FILE: test.kt

fun f(h: H) {
    h.writeDelimitedTo()
}

fun box(): String {
    f(H())
    return "OK"
}

