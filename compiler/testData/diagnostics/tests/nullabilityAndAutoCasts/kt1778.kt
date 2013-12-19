//KT-1778 Automatically cast error
package kt1778

fun main(args : Array<String>) {
    val x = args[0]: Any
    if(x is <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.CharSequence<!>) {
        if ("a" == x) <!DEBUG_INFO_AUTOCAST!>x<!>.length() else <!DEBUG_INFO_AUTOCAST!>x<!>.length() // OK
        if ("a" == x || "b" == x) <!DEBUG_INFO_AUTOCAST!>x<!>.length() else <!DEBUG_INFO_AUTOCAST!>x<!>.length() // <– THEN ERROR
        if ("a" == x && "a" == x) <!DEBUG_INFO_AUTOCAST!>x<!>.length() else <!DEBUG_INFO_AUTOCAST!>x<!>.length() // <– ELSE ERROR
    }
}