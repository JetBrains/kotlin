// TARGET_BACKEND: JVM
// FILE: KBase.kt
open class KBase {
    companion object {
        const val X = 1
    }
}

// FILE: J.java
public class J extends KBase {
    public static final int Y = X;
    public static final int Z = KBase.X;
}

// FILE: test.kt
const val KY = J.Y
const val KZ = J.Z

fun box(): String = if (KY == 1 && KZ == 1) "OK" else "FAIL: $KY $KZ"
