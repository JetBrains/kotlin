// PROBLEM: none
// WITH_RUNTIME
// ERROR: No value passed for parameter 'second'
// ERROR: Type inference failed: Not enough information to infer parameter B in constructor Pair<out A, out B>(first: A, second: B)<br>Please specify it explicitly.<br>
import kotlin.Pair
fun test() {
    val p = <caret>Pair(1, )
}