// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

// FILE: Test.kt
class TopLevelClass {
    var topLevelClassProperty: UInt = 1u
}

// FILE: Main.java
public class Main {
    public kotlin.UInt test() {
        return new TopLevelClass().getTopLevelClassProperty();
    }
}

// FILE: Box.kt
fun box(): String {
    if (Main().test() == 1u) return "OK"
    return "FAIL"
}
