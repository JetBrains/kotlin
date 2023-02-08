// IGNORE_BACKEND_K2: JVM_IR
// ^ See javaFields.kt for a copy of this test for K2.

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

// @Kotlin2.class:
// 1 GETFIELD Java2.f : I

// @TestKt.class:
// 1 GETFIELD Kotlin2.f : I
