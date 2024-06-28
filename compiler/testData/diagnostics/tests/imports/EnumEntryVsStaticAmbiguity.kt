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
        <!DEPRECATED_RESOLVE_WITH_AMBIGUOUS_ENUM_ENTRY!>SOME<!> -> true
        else -> false
    }
}
