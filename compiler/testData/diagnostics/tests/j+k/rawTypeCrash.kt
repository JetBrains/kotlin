// FIR_IDENTICAL
// ISSUE: KT-66552

// FILE: JsStubElementType.java
public interface JsStubElementType<E, S extends JsStubElement<E>> {}

// FILE: JsStubElement.java
public interface JsStubElement<T> {
    <E, S extends JsStubElement<E>> void findChildStubByType(JsStubElementType<E, S> childStubType);
}

// FILE: FromJava.java
public interface FromJava {
    JsStubElement RAW_TYPED_STUB = null;
}

// FILE: Kotlin.kt
interface JsStubElementSubType: JsStubElement<Any>

fun test(childStubType: JsStubElementType<Any, JsStubElement<Any>>) {
    val rawTypedStub = FromJava.RAW_TYPED_STUB
    if (rawTypedStub is JsStubElementSubType) {
        rawTypedStub.findChildStubByType(childStubType)
    }
}