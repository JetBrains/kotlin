// TARGET_BACKEND: JVM
// DUMP_EXTERNAL_CLASS: JEnum

// Minor bug: parameter types' flexibility mismatch
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: JEnum.java

public enum JEnum {
    ONE, TWO, THREE;
}

// FILE: javaEnum.kt

val test = JEnum.ONE
