// !WITH_NEW_INFERENCE
package noInformationForParameter
//+JDK

import java.util.*

fun test() {
    val n = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>newList<!>()

    val n1 : List<String> = newList()
}

fun <S> newList() : ArrayList<S> {
    return ArrayList<S>()
}
