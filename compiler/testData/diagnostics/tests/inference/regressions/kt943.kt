// FIR_IDENTICAL
// !CHECK_TYPE

//KT-943 Type inference failed
package maze

//+JDK
import java.util.Collections.*
import java.util.*
import checkSubtype

fun foo(lines: List<String>) {
    val w = max(lines, comparator {o1, o2 ->
        val l1 : Int = o1.length // Types of o1 and o2 are ERROR
        val l2 = o2.length
        l1 - l2
    }).sure()
    checkSubtype<String>(w)
}

//standard library
fun <T : Any> T?.sure() : T = this!!

public inline fun <T> comparator(fn: (T,T) -> Int): Comparator<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
