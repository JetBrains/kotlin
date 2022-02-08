// !CHECK_TYPE
// SKIP_TXT

// FILE: lib.kt
package test.abc

class V {
    companion object
}

val V.a: String
    get() = "1"

val V.Companion.a: Int
    get() = 1

// FILE: main.kt
import test.abc.V
import test.abc.a
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

fun case() {
    <!PARENTHESIZED_COMPANION_LHS_DEPRECATION!>(V)<!>::a checkType { _<KProperty0<Int>>() }
    <!PARENTHESIZED_COMPANION_LHS_DEPRECATION!>(V)<!>::a checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><KProperty1<V, String>>() }

    <!PARENTHESIZED_COMPANION_LHS_DEPRECATION!>(test.abc.V)<!>::a checkType { _<KProperty0<Int>>() }
    <!PARENTHESIZED_COMPANION_LHS_DEPRECATION!>(test.abc.V)<!>::a checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><KProperty1<V, String>>() }

    V::a checkType { _<KProperty1<V, String>>() }
    V.Companion::a checkType { _<KProperty0<Int>>() }

    (V.Companion)::a checkType { _<KProperty0<Int>>() }
}
