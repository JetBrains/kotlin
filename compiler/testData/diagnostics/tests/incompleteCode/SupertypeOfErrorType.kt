// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
package a

import java.util.Date
import java.util.Comparator

fun foo() {

    val c: Comparator<Date?> = comparator { date1, date2 ->
        if (date1 != null && date2 != null) {
            date1.compareTo(date2) * -11
        } else {
            11
        }
    }
}

fun bar(i: Int, a: <!UNRESOLVED_REFERENCE!>U<!>) {
    val r = if (true) i else <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
    val b: Any = r
}

//from standard library
public inline fun <T> comparator(fn: (T,T) -> Int): Comparator<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

/* GENERATED_FIR_TAGS: andExpression, equalityExpression, functionDeclaration, functionalType, ifExpression, inline,
integerLiteral, javaFunction, lambdaLiteral, localProperty, multiplicativeExpression, nullableType, propertyDeclaration,
smartcast, typeParameter */
