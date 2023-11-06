// FIR_IDENTICAL
// KT-49200
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

// FILE: third/OtherTypes.java

package third

public interface OtherTypes {
    String SOME = "Other";
}

// FILE: test.kt

import first.KtNodeTypes.*
import second.SomeEnum.*
import third.OtherTypes.*

fun test(arg: String): Boolean {
    return when (arg) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>SOME<!> -> true
        else -> false
    }
}
