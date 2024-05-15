// TARGET_BACKEND: JVM_IR

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

// 0 GETFIELD Java1.f : I

// @Kotlin2.class:
// 1 GETFIELD Java2.f : I

// @TestKt.class:
// 1 GETFIELD Kotlin2.f : I
