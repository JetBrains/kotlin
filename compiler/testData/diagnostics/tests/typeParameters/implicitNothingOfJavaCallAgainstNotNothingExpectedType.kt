// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNCHECKED_CAST -UNUSED_EXPRESSION -UNREACHABLE_CODE

// FILE: abc/Bar.java

package abc;

public class Bar {
    public static <T> T bar() {
        return null;
    }
}

// FILE: main.kt

import abc.Bar

fun ifProblem(b: Boolean): String? {
    return run {
        if (b) { Bar.<!IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE!>bar<!>() } else null
    }
}

fun whenProblem(b: Boolean): String? {
    return run {
        when {
            b -> Bar.<!IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE!>bar<!>()
            else -> null
        }
    }
}

fun tryProblem(): String? {
    return run {
        try {
            Bar.<!IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE!>bar<!>()
        } catch (e: Exception) {
            null
        }
    }
}