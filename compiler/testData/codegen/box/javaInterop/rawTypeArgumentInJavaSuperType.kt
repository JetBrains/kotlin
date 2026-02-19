// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// DUMP_IR
// DUMP_EXTERNAL_CLASS: StubSerializer
// FILE: StubElement.java
public interface StubElement<T> {}

// FILE: ObjectStubSerializer.java
public interface ObjectStubSerializer<P> {
    String deserialize(P parentStub);
}

// FILE: StubSerializer.java
public interface StubSerializer extends ObjectStubSerializer<StubElement> {
}

// FILE: 1.kt
class KtValueArgumentElementType<T> : StubSerializer {
    override fun deserialize(parentStub: StubElement<Any>?): String = "OK"
}

fun box(): String {
    val v = KtValueArgumentElementType<String>()
    v.deserialize(null)
    return (v as ObjectStubSerializer<*>).deserialize(null)
}
