// TARGET_BACKEND: JVM

// FILE: implicitNotNullInDestructuringAssignment.kt

// NB extension receiver is nullable
operator fun J?.component1() = 1

private operator fun J.component2() = 2

fun test() {
    val (a, b) = J.j()
}

// FILE: J.java
public class J {
    public static J j() { return null; }
}
