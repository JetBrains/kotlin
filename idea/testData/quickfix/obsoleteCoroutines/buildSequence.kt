// "Fix experimental coroutines usage" "true"
// ERROR: Unresolved reference: buildSequence
import kotlin.coroutines.<caret>experimental.buildSequence

fun main(args: Array<String>) {
    val lazySeq = buildSequence<Int> {
    }
}