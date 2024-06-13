// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

// Issue: KT-36254

// FILE: Convertor.java

public interface Convertor<Src, Dst> {
    Dst convert(Src o);
}

// FILE: main.kt

fun takeConvertor(c: Convertor<String, String>) {}

fun main() {
    takeConvertor(Convertor { null })
}
