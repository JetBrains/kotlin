// !DIAGNOSTICS: -UNUSED_VALUE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// JAVAC_EXPECTED_FILE
// FILE: MyMap.java

abstract public class MyMap extends java.util.AbstractMap<Double, CharSequence> {
    String get(Object q) { }
}

// FILE: main.kt

fun foo(m: MyMap) {
    var x: String? = m.get(1.0)
    x = m[2.0]
}
