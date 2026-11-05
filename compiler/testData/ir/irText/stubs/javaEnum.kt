// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: JEnum
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.

// FILE: JEnum.java

public enum JEnum {
    ONE, TWO, THREE;
}

// FILE: javaEnum.kt

val test = JEnum.ONE
