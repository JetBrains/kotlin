enum class A(val x: Boolean) {
    <!ELEMENT(1)!>(false),
    <!ELEMENT(2)!>(true);
}

fun box(): String? {
    if (A.<!ELEMENT(1)!>.x) return null
    if (!A.<!ELEMENT(2)!>.x) return null

    if (A.<!ELEMENT(1)!>.name != "<!ELEMENT_VALIDATION(1)!>") return null
    if (A.<!ELEMENT(2)!>.name != "<!ELEMENT_VALIDATION(2)!>") return null

    return "OK"
}