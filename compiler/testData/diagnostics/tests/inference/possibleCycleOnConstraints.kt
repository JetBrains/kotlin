// !WITH_NEW_INFERENCE
package a

import java.util.*

fun <T> g (<!UNUSED_PARAMETER!>f<!>: () -> List<T>) : T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun test() {
    //here possibly can be a cycle on constraints
    val <!UNUSED_VARIABLE!>x<!> = <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>g<!> { Collections.<!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>() }

    val <!UNUSED_VARIABLE!>y<!> = g<Int> { Collections.emptyList() }
    val <!UNUSED_VARIABLE!>z<!> : List<Int> = g { Collections.<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>() }
}