// !WITH_NEW_INFERENCE
// FULL_JDK

import java.util.concurrent.*

val concurrent: ConcurrentMap<String, Int> = null!!
val concurrentHash: ConcurrentHashMap<String, Int> = null!!

fun foo() {
    concurrent.remove("", 1)
    concurrent.remove("", <!ARGUMENT_TYPE_MISMATCH!>""<!>)
    concurrentHash.remove("", 1)
    concurrentHash.remove("", <!ARGUMENT_TYPE_MISMATCH!>""<!>)

    // Flexible types
    concurrent.remove(null, 1)
    concurrent.remove(null, null)

    // @PurelyImplements
    concurrentHash.remove(null, 1)
    concurrentHash.remove(null, null)
}
