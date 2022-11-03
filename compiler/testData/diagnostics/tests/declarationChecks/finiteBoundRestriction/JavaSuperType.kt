// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FILE: A.java
public class A<T extends A> {}

// FILE: 1.kt
<!FINITE_BOUNDS_VIOLATION_IN_JAVA!>class B<S: A<*>><!>