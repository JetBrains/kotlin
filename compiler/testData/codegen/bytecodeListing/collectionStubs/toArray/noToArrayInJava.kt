// FILE: test/JavaClass.java
package test;

public abstract class JavaClass<T> implements A<T> {

}

// FILE: main.kt
package test

interface A<T> : Collection<T>

// There must be toArray methods in B
abstract class B<E> : JavaClass<E>()
