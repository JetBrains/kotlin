package maze

//+JDK
import java.util.Collections.*
import java.util.*

fun foo(lines: List<String>) {
    val w = max(lines, comparator {o1, o2 ->
        val l1 : Int = o1.length // Types of o1 and o2 are ERROR
        val l2 = o2.length
        l1 - l2
    }).sure()
    w : String
}

//standard library
public inline fun <T> comparator(<!UNUSED_PARAMETER!>fn<!>: (T,T) -> Int): Comparator<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>