package i

//+JDK
import java.util.*

fun <T, R> Collection<T>.map1(<!UNUSED_PARAMETER!>f<!> : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun <T, R> <!CLASS_HAS_KOTLIN_ANALOG!>java.lang.Iterable<T><!>.map1(<!UNUSED_PARAMETER!>f<!> : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test(list: List<Int>) {
    val res = list.map1 { it }
    //check res is not of error type
    <!TYPE_MISMATCH!>res<!> : String
}

fun <T> Collection<T>.foo() {}
fun <T> <!CLASS_HAS_KOTLIN_ANALOG!>java.lang.Iterable<T><!>.foo() {}

fun test1(list: List<Int>) {
    val res = list.foo()
    //check res is not of error type
    <!TYPE_MISMATCH!>res<!> : String
}