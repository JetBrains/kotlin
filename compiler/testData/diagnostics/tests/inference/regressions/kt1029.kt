//KT-1029 Wrong type inference
package i

public fun<T> from(<!UNUSED_PARAMETER!>yielder<!>: ()->Iterable<T>) : Iterable<T> {
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

public fun<T> Iterable<T>.where(<!UNUSED_PARAMETER!>predicate<!> : (T)->Boolean) : ()->Iterable<T> {
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun a() {
    val x = 0..200
    val odd = from (x where {it%2==0}) // I believe it should infer here

    odd : Iterable<Int>
}
