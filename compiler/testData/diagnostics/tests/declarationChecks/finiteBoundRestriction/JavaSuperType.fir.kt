// FILE: A.java
public class A<T extends A> {}

// FILE: 1.kt
class B<S: A<*>>