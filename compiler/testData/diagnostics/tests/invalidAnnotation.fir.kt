// WITH_STDLIB
// ISSUE: KT-49962
// COMPARE_WITH_LIGHT_TREE

import java.io.*

class X<K, V> constructor() : Closeable {

    @Throws(IOException::<!UNRESOLVED_REFERENCE!>claut<!><!SYNTAX!>(key<!SYNTAX{LT}!>: K<!>, value<!SYNTAX{LT}!>: V<!>) {
    }<!><!SYNTAX!><!>

    @Throws(IOException::class)
    <!WRONG_MODIFIER_TARGET!>override<!> fun <!ANONYMOUS_FUNCTION_WITH_NAME!>close<!>() {}<!SYNTAX!><!>
}
