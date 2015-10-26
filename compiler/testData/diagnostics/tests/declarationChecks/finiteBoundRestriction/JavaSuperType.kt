// FILE: A.java
public class A<T extends A> {}

// FILE: 1.kt
<!FINITE_BOUNDS_VIOLATION!>class B<S: A<*>><!>