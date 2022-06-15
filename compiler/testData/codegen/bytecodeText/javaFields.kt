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

// JVM_IR_TEMPLATES
// @Kotlin2.class:
// 2 GETFIELD Java2.f : I

// JVM_IR_TEMPLATES
// @TestKt.class:
// 2 GETFIELD Java2.f : I

// JVM_TEMPLATES
// @Kotlin2.class:
// 1 GETFIELD Java2.f : I

// JVM_TEMPLATES
// @TestKt.class:
// 1 GETFIELD Java2.f : I