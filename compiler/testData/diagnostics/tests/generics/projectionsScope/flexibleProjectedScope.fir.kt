// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
// !CHECK_TYPE

// FILE: Clazz.java
public class Clazz<Psi> {
    public java.util.Collection<Psi> foo() { return null; }
}

// FILE: main.kt

public fun <T, C : MutableCollection<in T>> Iterable<T>.filterTo(destination: C, predicate: (T) -> Boolean) {}

fun test(clazz: Clazz<out Any>) {
    val result = java.util.<!UNRESOLVED_REFERENCE!>ArrayList<!><Any>()
    clazz.foo().<!INAPPLICABLE_CANDIDATE!>filterTo<!>(result) { x -> true }
}
