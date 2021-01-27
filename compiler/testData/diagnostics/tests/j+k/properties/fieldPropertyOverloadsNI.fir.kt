// SKIP_TXT
// !LANGUAGE: +PreferJavaFieldOverload +NewInference
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
    CompressionType.ZIP.<!AMBIGUITY!>name<!> <!UNSAFE_CALL!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Double>() }
    c.<!AMBIGUITY!>size<!> <!UNSAFE_CALL!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }

    <!UNRESOLVED_REFERENCE!>CompressionType.ZIP::name<!> <!UNSAFE_CALL!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><kotlin.reflect.KProperty0<Double>>() }
    <!UNRESOLVED_REFERENCE!>c::size<!> <!UNSAFE_CALL!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><kotlin.reflect.KProperty0<String>>() }
}
