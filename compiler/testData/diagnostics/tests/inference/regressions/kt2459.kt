// FIR_IDENTICAL
//KT-2459 Type inference error
package b

import java.util.*

class B<T>(val x: List<T>)
fun <T> f(x: T): B<T> = B(arrayList(x))

// from standard library
fun <T> arrayList(vararg values: T) : ArrayList<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
