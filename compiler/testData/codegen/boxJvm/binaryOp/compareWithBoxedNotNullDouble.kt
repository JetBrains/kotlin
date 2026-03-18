// TARGET_BACKEND: JVM
//FILE: Holder.java
import org.jetbrains.annotations.*;

class Holder {
    public @NotNull Double value;
    public Holder(Double value) { this.value = value; }
}

//FILE: test.kt

import Holder

fun box(): String {
    val j = Holder(0.99)
    return if (j.value > 0) "OK" else "fail"
}
