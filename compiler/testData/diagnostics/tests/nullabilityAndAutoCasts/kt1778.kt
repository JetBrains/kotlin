//KT-1778 Automatically cast error
package kt1778

fun main(args : Array<String>) {
    val x = args[0]: Any
    if(x is <!CLASS_HAS_KOTLIN_ANALOG!>java.lang.CharSequence<!>) {
        if ("a" == x) x.length() else x.length() // OK
        if ("a" == x || "b" == x) x.length() else x.length() // <– THEN ERROR
        if ("a" == x && "a" == x) x.length() else x.length() // <– ELSE ERROR
    }
}