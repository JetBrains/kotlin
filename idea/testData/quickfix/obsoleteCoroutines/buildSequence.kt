// "Fix experimental coroutines usage" "true"
// WITH_RUNTIME
package some

import kotlin.coroutines.experimental.buildSequence

fun main(args: Array<String>) {
    val lazySeq = <caret>buildSequence<Int> {
    }
}