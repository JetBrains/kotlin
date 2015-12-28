// FILE: A.java

import org.jetbrains.annotations.*;

public class A {
    @Nullable Base<@NotNull String> foo() { return null; }
}

// FILE: a.kt

interface Base<T> {}
interface Derived<E> : Base<E> {}

fun bar1(): Derived<String> = null!!
fun bar2(): Derived<String?> = null!!

class B : A() {
    override fun foo(): Base<String> { return bar1(); }
}

class C1 : A() {
    override fun foo(): Derived<String> { return bar1(); }
}

class C2 : A() {
    override fun foo(): Derived<String>? { return bar1(); }
}

class C3 : A() {
    override fun foo(): Derived<String?> { return bar2(); }
}
