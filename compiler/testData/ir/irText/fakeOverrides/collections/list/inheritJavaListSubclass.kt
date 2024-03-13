// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// FULL_JDK
// FILE: Java1.java

public interface Java1 extends java.util.List<String> {}

// FILE: 1.kt

interface A : Java1
