// !CHECK_TYPE

//KT-1031 Can't infer type of `it` with two lambdas
package i

import java.util.ArrayList

public infix fun<TItem> Iterable<TItem>.where(<!UNUSED_PARAMETER!>predicate<!> : (TItem)->Boolean) : ()->Iterable<TItem> {
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

public fun<TItem, TResult> select(<!UNUSED_PARAMETER!>yielder<!>: ()->Iterable<TItem>, <!UNUSED_PARAMETER!>selector<!> : (TItem)->TResult) : ()->Iterable<TResult> {
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun a() {
    val x = 0..200
    val z = x where { i: Int -> i % 2 == 0 }
    val yielder = select(x where { it%2==0 }, { it.toString() })

    checkSubtype<() -> Iterable<Int>>(z)
    checkSubtype<() -> Iterable<String>>(yielder)
}
