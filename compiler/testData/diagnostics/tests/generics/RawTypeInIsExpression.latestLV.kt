// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
package p

public fun foo(a: Any) {
    a is Map<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>
    a is Map
    a is Map<out Any?, Any?>
    a is Map<*, *>
    a is Map<<!SYNTAX!><!>>
    a is List<<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Map<!>>
    a is List
    a is Int

    <!USELESS_IS_CHECK!>(a as Map) is Int<!>
}
