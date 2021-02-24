// FILE: JavaClass.java
public class JavaClass {
    public static int bar(String x) { return 0; }
    public int bar(CharSequence x) { return 0; }
}

// FILE: main.kt

fun foo1(x: (String) -> Int) {}

fun foo2(x: (JavaClass, CharSequence) -> Int) {}

fun foo3(x: (JavaClass, CharSequence) -> Int) {}
fun foo3(x: (String) -> Int) {}

fun main() {
    foo1(JavaClass::bar)
    foo2(JavaClass::bar)
    <!AMBIGUITY!>foo3<!>(JavaClass::bar)
}
