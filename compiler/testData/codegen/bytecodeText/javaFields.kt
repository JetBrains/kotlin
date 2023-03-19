// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// ^ See javaFields_k1.kt for a copy of this test for K1.

// FILE: Java1.java
public class Java1 { 
  public int f;
}

// FILE: Java2.java
public class Java2 extends Kotlin1 {
}

// FILE: test.kt
open class Kotlin1 : Java1()

open class Kotlin2 : Java2() {
  fun getF() = super.f
}

fun test1(j: Kotlin2) = j.f

// K2 generates access to Java1.f in both cases. The main motivation for this is to fix cases like KT-49507.
// Java1 in this case is the most specific Java superclass of Kotlin2 which has no Kotlin superclasses in its hierarchy.

// 2 GETFIELD Java1.f : I
// 0 GETFIELD Java2.f : I
// 0 GETFIELD Kotlin2.f : I
