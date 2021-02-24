// TARGET_BACKEND: JVM
//FILE: JavaClass.java
import org.jetbrains.annotations.*;

class JavaClass {
    public static @NotNull Long get() { return 2364137526064485012L; }
}

//FILE: test.kt

import JavaClass

fun box(): String {
    return if (JavaClass.get() > 0) "OK" else "fail"
}
