// !CHECK_TYPE
// WITH_EXTENDED_CHECKERS
//KT-1778 Automatically cast error
package kt1778

import checkSubtype

fun main(args : Array<String>) {
    val x = checkSubtype<Any>(args[0])
    if(x is <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.CharSequence<!>) {
        if (<!EQUALITY_NOT_APPLICABLE_WARNING!>"a" == x<!>) x.<!FUNCTION_CALL_EXPECTED!>length<!> else x.length() // OK
        if (<!EQUALITY_NOT_APPLICABLE_WARNING!>"a" == x<!> || <!EQUALITY_NOT_APPLICABLE_WARNING!>"b" == x<!>) x.<!FUNCTION_CALL_EXPECTED!>length<!> else x.length() // <– THEN ERROR
        if (<!EQUALITY_NOT_APPLICABLE_WARNING!>"a" == x<!> && <!EQUALITY_NOT_APPLICABLE_WARNING!>"a" == x<!>) x.<!FUNCTION_CALL_EXPECTED!>length<!> else x.length() // <– ELSE ERROR
    }
}
