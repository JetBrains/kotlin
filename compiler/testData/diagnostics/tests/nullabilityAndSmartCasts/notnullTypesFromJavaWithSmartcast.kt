// FILE: JClass.java

import org.jetbrains.annotations.NotNull;

public class JClass {
    @NotNull
    public static <T> T getNotNullT() {
        return null;
    }
}

// FILE: test.kt
fun <T : Any> test() {
    var value: T? = null
    if (value == null) {
        value = JClass.getNotNullT()
    }

    <!DEBUG_INFO_SMARTCAST!>value<!>.hashCode() // unsafe call error
}