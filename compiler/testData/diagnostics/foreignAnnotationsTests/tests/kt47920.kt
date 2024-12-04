// FIR_IDENTICAL
// MUTE_FOR_PSI_CLASS_FILES_READING
// SKIP_TXT

// FILE: J1.java
import io.reactivex.rxjava3.annotations.*;

public class J1<@NonNull T> {}

// FILE: main.kt
fun main() {
    J1<<!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS!>Any?<!>>() // violated nullability, no warnings; but there is an error with -Xtype-enhancement-improvements-strict-mode
}
