package noInformationForParameter
//+JDK

import java.util.*

fun test() {
    val <!UNUSED_VARIABLE!>n<!> = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>newList<!>()

    val <!UNUSED_VARIABLE!>n1<!> : List<String> = newList()
}

fun newList<S>() : ArrayList<S> {
    return ArrayList<S>()
}