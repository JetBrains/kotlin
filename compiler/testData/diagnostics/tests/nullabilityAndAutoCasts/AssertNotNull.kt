fun main(args : Array<String>) {
    val a : Int? = null
    val b : Int? = null
    a!! : Int
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

    if (d is String) {
        d<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }

    if (d is String?) {
        if (d != null) {
            d<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
        }
        if (d is String) {
            d<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
        }
    }

    val <!UNUSED_VARIABLE!>f<!> : String = <!TYPE_MISMATCH!>a<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    <!TYPE_MISMATCH!>a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!> : String
}