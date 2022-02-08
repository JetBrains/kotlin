// !LANGUAGE: -PreferJavaFieldOverload

// SKIP_TXT
// !CHECK_TYPE

// FILE: CompressionType.java
public enum CompressionType {
    ZIP(1.0);
    public final double name;
    CompressionType(double name) {
        this.name = name;
    }
}

// FILE: CollectionWithSize.java
public abstract class CollectionWithSize implements java.util.Collection<String> {
    public final String size = "";
}

// FILE: main.kt

fun main(c: CollectionWithSize) {
    CompressionType.ZIP.<!OVERLOAD_RESOLUTION_AMBIGUITY!>name<!> <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<Double>() }
    c.<!OVERLOAD_RESOLUTION_AMBIGUITY!>size<!> <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<String>() }

    CompressionType.ZIP::<!OVERLOAD_RESOLUTION_AMBIGUITY!>name<!> <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<kotlin.reflect.KProperty0<Double>>() }
    c::<!OVERLOAD_RESOLUTION_AMBIGUITY!>size<!> <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<kotlin.reflect.KProperty0<String>>() }
}
