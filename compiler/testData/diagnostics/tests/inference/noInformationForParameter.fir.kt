// RUN_PIPELINE_TILL: FRONTEND
package noInformationForParameter
//+JDK

import java.util.*

fun test() {
    val n = <!CANNOT_INFER_PARAMETER_TYPE!>newList<!>()

    val n1 : List<String> = newList()
}

fun <S> newList() : ArrayList<S> {
    return ArrayList<S>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, localProperty, nullableType, propertyDeclaration,
typeParameter */
