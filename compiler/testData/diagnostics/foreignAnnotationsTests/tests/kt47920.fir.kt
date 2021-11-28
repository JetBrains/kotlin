// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: J1.java
import io.reactivex.rxjava3.annotations.*;

public class J1<@NonNull T> {}

// FILE: main.kt
fun main() {
    J1<Any?>() // violated nullability, no warnings; but there is an error with -Xtype-enhancement-improvements-strict-mode
}
