// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

// Issue: KT-36254

// FILE: Convertor.java

public interface Convertor<Src, Dst> {
    Out<Dst> convert(Out<Src> o);
}

// FILE: main.kt

fun takeConvertor(c: Convertor<String, String>) {}

class Out<out T> {}

fun main(o: Out<Nothing?>) {
    takeConvertor(Convertor { o })
}
