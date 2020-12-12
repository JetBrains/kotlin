// !CHECK_TYPE

//KT-2176 non-nullability is not inferred after !! or "as"
package kt2176

import checkSubtype

fun f1(a: String?) {
    a!!
    checkSubtype<String>(a)
}

fun f2(a: String) {
    a!!
    checkSubtype<String>(a)
}

fun f3(a: Any?) {
    a as String
    checkSubtype<String>(a)
}

fun f4(a: Any) {
    a as String
    checkSubtype<String>(a)
}

fun f5(a: String) {
    a as Any?
    checkSubtype<String>(a)
}
