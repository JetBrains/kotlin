// TARGET_BACKEND: JVM
// FILE: kt43242.kt

fun fromJson() {
    test = Bar().fromJson()?.let {
        when (it) {
            0 -> false
            1 -> true
            else -> true
        }
    }
}

var test: Any? = "xxx"

fun box(): String {
    fromJson()
    return if (test != null) "Fail: $test" else "OK"
}

// FILE: Bar.java
import org.jetbrains.annotations.Nullable;

public class Bar {
    public final @Nullable Integer fromJson() {
        return null;
    }
}