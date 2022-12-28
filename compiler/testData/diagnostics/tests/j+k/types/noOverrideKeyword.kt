// !SKIP_JAVAC
// SKIP_TXT
// !LANGUAGE: -ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// FILE: JavaInterface.java
import org.jetbrains.annotations.NotNull;

public interface JavaInterface<V> {
    void interfaceMethod(@NotNull V value);
}

// FILE: main.kt
interface KotlinInterface<X> : JavaInterface<X> {
    override <!SYNTAX!><<!><!SYNTAX!>T<!><!SYNTAX!>><!> fun <!VIRTUAL_MEMBER_HIDDEN!>interfaceMethod<!>(x: X)
}
