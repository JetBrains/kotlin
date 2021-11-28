// !CHECK_TYPE

package a

//+JDK
import java.util.*
import checkSubtype

fun foo() {
    val v = array(1, 2, 3)

    val u = v map { it * 2 }

    checkSubtype<List<Int>>(u)

    val a = 1..5

    val b = a.map { it * 2 }

    checkSubtype<List<Int>>(b)

    //check for non-error types
    checkSubtype<String>(<!ARGUMENT_TYPE_MISMATCH!>u<!>)
    checkSubtype<String>(<!ARGUMENT_TYPE_MISMATCH!>b<!>)
}


// ---------------------
// copy from kotlin util (but with `infix` modifier on `map`)

@Suppress("UNCHECKED_CAST")
fun <T> array(vararg t : T) : Array<T> = t as Array<T>

infix fun <T, R> Array<T>.map(transform : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

infix fun <T, R> Iterable<T>.map(transform : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
