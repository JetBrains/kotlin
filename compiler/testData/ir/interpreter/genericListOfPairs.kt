import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
fun <F, S> listOfPairs(): String {
    val list = mutableListOf<Pair<F, S>>()
    list.add((1 to 100) as Pair<F, S>)
    list.add(("2" to "200") as Pair<F, S>)
    return "List size is: ${list.size}; first is Int: ${list[0].first is Int}; second is String: ${list[1].first is String}"
}
const val listOfPairsSize = <!EVALUATED: `List size is: 2; first is Int: true; second is String: true`!>listOfPairs<Int, Int>()<!>
