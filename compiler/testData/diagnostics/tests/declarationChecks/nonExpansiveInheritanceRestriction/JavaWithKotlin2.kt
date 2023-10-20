// FIR_IDENTICAL
// !DIAGNOSTICS: -UPPER_BOUND_VIOLATED

// FILE: D.java
public interface D<W> {}

// FILE: Q.java
public interface Q<Z1, Z2> {}

// FILE: C.java
public interface C<X> extends D<P<X,X>> {}

// FILE: P.java
public interface P<Y1, Y2> extends Q<C<Y1>, C<D<Y2>>> {}

// FILE: 1.kt
<!EXPANSIVE_INHERITANCE_IN_JAVA!>interface P1<YY1, YY2> : P<YY1, YY2><!>
