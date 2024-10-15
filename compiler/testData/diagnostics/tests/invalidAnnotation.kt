// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-49962
// COMPARE_WITH_LIGHT_TREE
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

import java.io.*

class X<K, V> constructor() : Closeable {

    @Throws(IOException::claut(key: K, value: V) {
    }

    @Throws(IOException::class)
    override fun close() {}
}