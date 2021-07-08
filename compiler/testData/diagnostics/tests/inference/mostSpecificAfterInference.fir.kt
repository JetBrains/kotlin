// !CHECK_TYPE

package i

//+JDK
import java.util.*
import checkSubtype

fun <T, R> Collection<T>.map1(f : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <T, R> java.lang.Iterable<T>.map1(f : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test(list: List<Int>) {
    val res = list.map1 { it }
    //check res is not of error type
    checkSubtype<String>(<!ARGUMENT_TYPE_MISMATCH!>res<!>)
}

fun <T> Collection<T>.foo() {}
fun <T> java.lang.Iterable<T>.foo() {}

fun test1(list: List<Int>) {
    val res = list.foo()
    //check res is not of error type
    checkSubtype<String>(<!ARGUMENT_TYPE_MISMATCH!>res<!>)
}
