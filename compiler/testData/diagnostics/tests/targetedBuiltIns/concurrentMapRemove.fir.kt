// !WITH_NEW_INFERENCE
// FULL_JDK

import java.util.concurrent.*

val concurrent: ConcurrentMap<String, Int> = null!!
val concurrentHash: ConcurrentHashMap<String, Int> = null!!

fun foo() {
    concurrent.remove("", 1)
    concurrent.<!INAPPLICABLE_CANDIDATE!>remove<!>("", "")
    concurrentHash.remove("", 1)
    concurrentHash.<!INAPPLICABLE_CANDIDATE!>remove<!>("", "")

    // Flexible types
    concurrent.remove(null, 1)
    concurrent.remove(null, null)

    // @PurelyImplements
    concurrentHash.remove(null, 1)
    concurrentHash.remove(null, null)
}
