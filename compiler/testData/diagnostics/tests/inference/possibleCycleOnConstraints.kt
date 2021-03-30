// !WITH_NEW_INFERENCE
package a

import java.util.*

fun <T> g (f: () -> List<T>) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test() {
    //here possibly can be a cycle on constraints
    val x = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}!>g<!> { Collections.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>emptyList<!>() }

    val y = g<Int> { Collections.emptyList() }
    val z : List<Int> = g { Collections.<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>emptyList<!>() }
}
