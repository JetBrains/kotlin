// LL_FIR_DIVERGENCE
// Different syntax errors
// LL_FIR_DIVERGENCE
// WITH_STDLIB
// ISSUE: KT-49962
// COMPARE_WITH_LIGHT_TREE

import java.io.*

class X<K, V> constructor() : Closeable {

    @Throws(IOException::<!UNRESOLVED_REFERENCE!>claut<!><!SYNTAX!>(key: K, value: V) {
    }<!><!SYNTAX!><!>

    @Throws(IOException::class)
    <!WRONG_MODIFIER_TARGET{LT}!>override<!> fun <!ANONYMOUS_FUNCTION_WITH_NAME{LT}!><!ANONYMOUS_FUNCTION_WITH_NAME!>close<!>() {}<!><!SYNTAX!><!>
}
