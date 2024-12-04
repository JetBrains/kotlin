// TARGET_BACKEND: JVM_IR
// ISSUE: KT-63588

// FILE: Base.java
public class Base {
    public String getBounds() { return ""; }
}

// FILE: Intermediate.java
public class Intermediate extends Base {}

// FILE: Final.java
public class Final extends Intermediate implements WithBounds {}

// FILE: Main.kt
interface WithBounds {
    val bounds: String
}

fun foo(arg: Final) {
    arg.bounds
}

fun box(): String {
    foo(Final())
    return "OK"
}
