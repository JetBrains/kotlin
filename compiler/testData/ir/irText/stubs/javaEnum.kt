// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: JEnum

// FILE: JEnum.java

public enum JEnum {
    ONE, TWO, THREE;
}

// FILE: javaEnum.kt

val test = JEnum.ONE
