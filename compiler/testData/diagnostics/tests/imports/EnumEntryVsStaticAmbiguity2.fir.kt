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

// FILE: OtherEnum.kt

package third

enum class SomeEnum {
    SOME;
}

// FILE: test.kt

import first.KtNodeTypes.*
import second.SomeEnum.*
import third.SomeEnum.*

fun test(arg: String): Boolean {
    return when (arg) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>SOME<!> -> true
        else -> false
    }
}
