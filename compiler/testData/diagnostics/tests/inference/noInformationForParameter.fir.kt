// !WITH_NEW_INFERENCE
package noInformationForParameter
//+JDK

import java.util.*

fun test() {
    val n = newList()

    val n1 : List<String> = newList()
}

fun <S> newList() : ArrayList<S> {
    return ArrayList<S>()
}