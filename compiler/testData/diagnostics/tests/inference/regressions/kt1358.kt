//KT-1358 Overload resolution ambiguity with autocast and generic function
package d

fun bar(a: Any?) {
    if (a != null) {
        <!DEBUG_INFO_AUTOCAST!>a<!>.foo() //overload resolution ambiguity
        <!DEBUG_INFO_AUTOCAST!>a<!>.sure() //overload resolution ambiguity
    }
}

fun <T : Any> T?.foo() {}
fun <T : Any> T?.sure() : T = this!!