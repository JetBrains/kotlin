//KT-2176 non-nullability is not inferred after !! or "as"
package kt2176

fun f1(a: String?) {
    a!!
    <!DEBUG_INFO_SMARTCAST!>a<!>: String
}

fun f2(a: String) {
    a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    a: String
}

fun f3(a: Any?) {
    a as String
    <!DEBUG_INFO_SMARTCAST!>a<!>: String
}

fun f4(a: Any) {
    a as String
    <!DEBUG_INFO_SMARTCAST!>a<!>: String
}

fun f5(a: String) {
    a <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> Any?
    a: String
}
