// !CHECK_TYPE
//KT-1778 Automatically cast error
package kt1778

fun main(args : Array<String>) {
    val x = checkSubtype<Any>(args[0])
    if(x is <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.CharSequence<!>) {
        if ("a" == x) <!DEBUG_INFO_SMARTCAST!>x<!>.length else <!DEBUG_INFO_SMARTCAST!>x<!>.length() // OK
        if ("a" == x || "b" == x) <!DEBUG_INFO_SMARTCAST!>x<!>.length else <!DEBUG_INFO_SMARTCAST!>x<!>.length() // <– THEN ERROR
        if ("a" == x && "a" == x) <!DEBUG_INFO_SMARTCAST!>x<!>.length else <!DEBUG_INFO_SMARTCAST!>x<!>.length() // <– ELSE ERROR
    }
}