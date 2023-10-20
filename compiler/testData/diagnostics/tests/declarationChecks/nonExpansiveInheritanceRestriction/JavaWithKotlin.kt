// FIR_IDENTICAL
// !DIAGNOSTICS: -UPPER_BOUND_VIOLATED

// FILE: D.java
public interface D<W> {}

// FILE: Q.java
public interface Q<Z1, Z2> {}

// FILE: C.java
public interface C<X> extends D<P<X,X>> {}

// FILE: 1.kt
interface P<Y1, <!EXPANSIVE_INHERITANCE!>Y2<!>> : Q<C<Y1>, C<D<Y2>>>

