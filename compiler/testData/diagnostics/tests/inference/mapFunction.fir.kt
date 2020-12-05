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
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><String>(u)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><String>(b)
}


// ---------------------
// copy from kotlin util (but with `infix` modifier on `map`)

@Suppress("UNCHECKED_CAST")
fun <T> array(vararg t : T) : Array<T> = t as Array<T>

infix fun <T, R> Array<T>.map(transform : (T) -> R) : List<R> {}

infix fun <T, R> Iterable<T>.map(transform : (T) -> R) : List<R> {}
