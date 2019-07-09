// !WITH_NEW_INFERENCE
// !CHECK_TYPE

fun main() {
    val a : Int? = null
    val b : Int? = null
    checkSubtype<Int>(a!!)
    a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> + 2
    a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.plus(2)
    a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.plus(b!!)
    2.plus(b<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    2 + b<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>

    val c = 1
    c<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>

    val d : Any? = null

    if (d != null) {
        d<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }

    // smart cast isn't needed, but is reported due to KT-4294
    if (d is String) {
        <!DEBUG_INFO_SMARTCAST!>d<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }

    if (d is String?) {
        if (d != null) {
            <!DEBUG_INFO_SMARTCAST!>d<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
        }
        if (d is String) {
            <!DEBUG_INFO_SMARTCAST!>d<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
        }
    }

    val <!UNUSED_VARIABLE!>f<!> : String = <!NI;TYPE_MISMATCH!><!OI;TYPE_MISMATCH!>a<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>
    checkSubtype<String>(<!NI;TYPE_MISMATCH!><!OI;TYPE_MISMATCH!>a<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>)
}