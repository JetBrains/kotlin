// !LANGUAGE: +DefinitelyNonNullableTypes +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated

// FILE: A.java
import org.jetbrains.annotations.*;

public interface A<T> {
    public T foo(T x) { return x; }
    @NotNull
    public T bar(@NotNull T x) {}
}

// FILE: main.kt

interface B<T1> : A<T1> {
    override fun foo(x: T1): T1
    override fun bar(x: T1 & Any): T1 & Any
}

interface C<T2> : A<T2> {
    override fun foo(x: T2 & Any): T2 & Any
    <!NOTHING_TO_OVERRIDE!>override<!> fun bar(x: T2): T2
}

interface D : A<String?> {
    override fun foo(x: String?): String?
    override fun bar(x: String): String
}

interface E : A<String> {
    override fun foo(x: String): String
    override fun bar(x: String): String
}

interface F : A<String?> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(x: String): String
    <!NOTHING_TO_OVERRIDE!>override<!> fun bar(x: String?): String?
}

interface G<T3 : Any> : A<T3> {
    override fun foo(x: T3): T3
    override fun bar(x: T3): T3
}
