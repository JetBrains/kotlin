// FIR_IDENTICAL
package a

import java.util.*

fun <T> g (f: () -> List<T>) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test() {
    //here possibly can be a cycle on constraints
    val x = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>g<!> { Collections.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>() }

    val y = g<Int> { Collections.emptyList() }
    val z : List<Int> = g { Collections.emptyList() }
}
