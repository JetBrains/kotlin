// !CHECK_TYPE

fun foo(x: Cloneable) = x

fun test() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(arrayOf(""))
    <!INAPPLICABLE_CANDIDATE!>foo<!>(intArrayOf())
    <!INAPPLICABLE_CANDIDATE!>foo<!>(longArrayOf())
    <!INAPPLICABLE_CANDIDATE!>foo<!>(shortArrayOf())
    <!INAPPLICABLE_CANDIDATE!>foo<!>(byteArrayOf())
    <!INAPPLICABLE_CANDIDATE!>foo<!>(charArrayOf())
    <!INAPPLICABLE_CANDIDATE!>foo<!>(doubleArrayOf())
    <!INAPPLICABLE_CANDIDATE!>foo<!>(floatArrayOf())
    <!INAPPLICABLE_CANDIDATE!>foo<!>(booleanArrayOf())

    arrayOf("").<!UNRESOLVED_REFERENCE!>clone<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Array<String>>() }
    intArrayOf().<!UNRESOLVED_REFERENCE!>clone<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><IntArray>() }
    longArrayOf().<!UNRESOLVED_REFERENCE!>clone<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><LongArray>() }
    shortArrayOf().<!UNRESOLVED_REFERENCE!>clone<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><ShortArray>() }
    byteArrayOf().<!UNRESOLVED_REFERENCE!>clone<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><ByteArray>() }
    charArrayOf().<!UNRESOLVED_REFERENCE!>clone<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><CharArray>() }
    doubleArrayOf().<!UNRESOLVED_REFERENCE!>clone<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><DoubleArray>() }
    floatArrayOf().<!UNRESOLVED_REFERENCE!>clone<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><FloatArray>() }
    booleanArrayOf().<!UNRESOLVED_REFERENCE!>clone<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><BooleanArray>() }
}
