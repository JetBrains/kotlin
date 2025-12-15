// JSPECIFY_STATE: strict
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83031
// FIR_DUMP

// FILE: P.java
import org.jspecify.annotations.*;

public class P {
    public static @NonNull String @NonNull [] f() {
        return new String[0];
    }
}

// FILE: C.kt
class C {
    fun g() {
        r() <!USELESS_CAST!>as String<!>
        for (p in P.f()) {
            // Should be @NonNull String
            <!DEBUG_INFO_EXPRESSION_TYPE("@org.jspecify.annotations.NonNull kotlin.String")!>p<!>.length     // <------ null check or not?
        }
    }

    fun r(): String = ""
}

// FILE: B.kt
class B {
    val c = arrayOf("")
    val t = c.size
}
