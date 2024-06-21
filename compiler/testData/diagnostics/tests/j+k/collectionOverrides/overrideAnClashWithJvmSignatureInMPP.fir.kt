// WITH_STDLIB
// FULL_JDK
// RENDER_DIAGNOSTICS_MESSAGES
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: +MultiPlatformProjects
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_REVERSED_RESOLVE

// MODULE: common
// FILE: Common.kt
expect abstract class B() {
    open fun get(index: Int): Char
}

expect abstract class KA : B {
    override fun <!ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE("'fun get(index: Int): Char' defined in '/A'; a renamed function; 'fun charAt(index: Int): Char' defined in '/A'")!>get<!>(index: Int): Char
}

abstract class KA2() : B() {
    override fun <!ACCIDENTAL_OVERRIDE_CLASH_BY_JVM_SIGNATURE("'fun get(index: Int): Char' defined in '/A'; a renamed function; 'fun charAt(index: Int): Char' defined in '/A'")!>get<!>(index: Int): Char = 'A'
}

// MODULE: jvm()()(common)
// FILE: A.java
abstract public class A implements CharSequence {
    public final int length() {
        return 0;
    }

    public char charAt(int index) {
        return ' ';
    }

    public char get(int index) {
        return 'X';
    }
}

// FILE: main.kt

actual abstract class B : A()
actual abstract class KA : B()

fun foo(a: A, ka: KA) {
    a.get(0)
    ka.get(0)
}
