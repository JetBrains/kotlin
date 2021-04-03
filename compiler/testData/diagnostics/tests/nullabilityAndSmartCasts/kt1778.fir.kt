// !CHECK_TYPE
//KT-1778 Automatically cast error
package kt1778

import checkSubtype

fun main(args : Array<String>) {
    val x = checkSubtype<Any>(args[0])
    if(x is java.lang.CharSequence) {
        if (<!EQUALITY_NOT_APPLICABLE!>"a" == x<!>) x.<!UNRESOLVED_REFERENCE!>length<!> else x.length() // OK
        if (<!EQUALITY_NOT_APPLICABLE!>"a" == x<!> || <!EQUALITY_NOT_APPLICABLE!>"b" == x<!>) x.<!UNRESOLVED_REFERENCE!>length<!> else x.length() // <– THEN ERROR
        if (<!EQUALITY_NOT_APPLICABLE!>"a" == x<!> && <!EQUALITY_NOT_APPLICABLE!>"a" == x<!>) x.<!UNRESOLVED_REFERENCE!>length<!> else x.length() // <– ELSE ERROR
    }
}
