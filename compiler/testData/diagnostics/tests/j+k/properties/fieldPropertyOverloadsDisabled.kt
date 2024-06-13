// LANGUAGE: -PreferJavaFieldOverload

// SKIP_TXT
// CHECK_TYPE

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
    CompressionType.ZIP.<!OVERLOAD_RESOLUTION_AMBIGUITY!>name<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Double>() }
    c.<!OVERLOAD_RESOLUTION_AMBIGUITY!>size<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    CompressionType.ZIP::<!OVERLOAD_RESOLUTION_AMBIGUITY!>name<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><kotlin.reflect.KProperty0<Double>>() }
    c::<!OVERLOAD_RESOLUTION_AMBIGUITY!>size<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><kotlin.reflect.KProperty0<String>>() }
}
