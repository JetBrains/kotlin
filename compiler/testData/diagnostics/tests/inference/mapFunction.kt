package a

//+JDK
import java.util.*

fun foo() {
    val v = array(1, 2, 3)

    val u = v map { it * 2 }

    u : List<Int>

    val a = 1..5

    val b = a.map { it * 2 }

    b : List<Int>

    //check for non-error types
    <!TYPE_MISMATCH!>u<!> : String
    <!TYPE_MISMATCH!>b<!> : String
}


// ---------------------
// copy from kotlin util

fun <T> array(vararg t : T) : Array<T> = t as Array<T>

fun <T, R> Array<T>.map(<!UNUSED_PARAMETER!>transform<!> : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun <T, R> Iterable<T>.map(<!UNUSED_PARAMETER!>transform<!> : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>