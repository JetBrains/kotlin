// !CHECK_TYPE
//KT-1778 Automatically cast error
package kt1778

import checkSubtype

fun main(args : Array<String>) {
    val x = checkSubtype<Any>(args[0])
    if(x is java.lang.CharSequence) {
        if ("a" == x) x.<!UNRESOLVED_REFERENCE!>length<!> else x.length() // OK
        if ("a" == x || "b" == x) x.<!UNRESOLVED_REFERENCE!>length<!> else x.length() // <– THEN ERROR
        if ("a" == x && "a" == x) x.<!UNRESOLVED_REFERENCE!>length<!> else x.length() // <– ELSE ERROR
    }
}
