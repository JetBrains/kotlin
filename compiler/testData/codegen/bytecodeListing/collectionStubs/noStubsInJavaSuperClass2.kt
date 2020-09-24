// WITH_RUNTIME
// IGNORE_BACKEND: JVM
// ^ see KT-42179

// FILE: test/JC.java
package test;

import java.util.Collection;

public abstract class JC<E> implements Collection<E> {
}

// FILE: noStubsInJavaSuperClass2.kt
package test

abstract class KSet<E> : JC<E>(), Set<E>

abstract class KList<E> : JC<E>(), List<E>