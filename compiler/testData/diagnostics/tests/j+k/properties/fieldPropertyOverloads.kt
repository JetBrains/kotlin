// FIR_IDENTICAL
// SKIP_TXT
// LANGUAGE: +PreferJavaFieldOverload
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
    CompressionType.ZIP.name checkType { _<Double>() }
    c.size checkType { _<String>() }

    CompressionType.ZIP::name checkType { _<kotlin.reflect.KProperty0<Double>>() }
    c::size checkType { _<kotlin.reflect.KProperty0<String>>() }
}
