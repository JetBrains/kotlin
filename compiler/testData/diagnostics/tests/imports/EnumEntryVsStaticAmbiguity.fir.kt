// KT-49200
// RENDER_DIAGNOSTICS_FULL_TEXT
// FILE: first/KtNodeTypes.java

package first;

public interface KtNodeTypes {
    String SOME = "Some";
}

// FILE: SomeEnum.kt

package second

enum class SomeEnum {
    SOME;
}

// FILE: test.kt

import first.KtNodeTypes.*
import second.SomeEnum.*

fun test(arg: String): Boolean {
    return when (arg) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>SOME<!> -> true
        else -> false
    }
}
