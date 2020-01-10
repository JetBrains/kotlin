// FILE: JavaClass.java
public class JavaClass {
    public static int bar(String x) { return 0; }
    public int bar(CharSequence x) { return 0; }
}

// FILE: main.kt

class KotlinClass : JavaClass() {
    fun baz(x: CharSequence): Int = 1
    companion object {
        fun baz(x: String): Int = 1
    }
}

class KotlinClass2 : JavaClass() {
    override fun bar(x: CharSequence): Int = 1
    companion object {
        fun bar(x: String): Int = 1
    }
}

fun foo1(x: (String) -> Int) {}

fun foo2(x: (KotlinClass, CharSequence) -> Int) {}

fun foo3(x: (KotlinClass, CharSequence) -> Int) {}
fun foo3(x: (String) -> Int) {}

fun main() {
    foo1(KotlinClass::baz)
    foo2(KotlinClass::baz)
    // Ambiguity (companion/class)
    <!AMBIGUITY!>foo3<!>(KotlinClass::baz)

    // Type mismatch
    <!INAPPLICABLE_CANDIDATE!>foo1<!>(KotlinClass::bar)
    foo2(KotlinClass::bar)
    foo3(KotlinClass::bar)

    foo1(KotlinClass2::bar)
    // Type mismatch
    <!INAPPLICABLE_CANDIDATE!>foo2<!>(KotlinClass2::bar)
    foo3(KotlinClass2::bar)
}

