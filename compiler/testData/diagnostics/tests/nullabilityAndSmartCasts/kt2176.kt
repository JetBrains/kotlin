// !CHECK_TYPE

//KT-2176 non-nullability is not inferred after !! or "as"
package kt2176

fun f1(a: String?) {
    a!!
    checkSubtype<String>(<!DEBUG_INFO_SMARTCAST!>a<!>)
}

fun f2(a: String) {
    a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    checkSubtype<String>(a)
}

fun f3(a: Any?) {
    a as String
    checkSubtype<String>(<!DEBUG_INFO_SMARTCAST!>a<!>)
}

fun f4(a: Any) {
    a as String
    checkSubtype<String>(<!DEBUG_INFO_SMARTCAST!>a<!>)
}

fun f5(a: String) {
    a <!USELESS_CAST!>as Any?<!>
    checkSubtype<String>(a)
}
