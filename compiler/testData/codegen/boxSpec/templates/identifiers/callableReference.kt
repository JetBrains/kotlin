<!DIRECTIVES("HELPERS: REFLECT")!>

val Boolean.<!ELEMENT(1)!>: Boolean
    get() {
        return true
    }

val Boolean?.<!ELEMENT(2)!>: Boolean
    get() {
        return false
    }

fun Int?.<!ELEMENT(3)!>(x: Boolean): Boolean {
    return !x
}

fun box(): String? {
    if (!false.<!ELEMENT(1)!> || false.<!ELEMENT(2)!> || !0.<!ELEMENT(3)!>(false)) return null

    if (!checkCallableName(Boolean::<!ELEMENT(1)!>, "<!ELEMENT_VALIDATION(1)!>") || !Boolean::<!ELEMENT(1)!>.get(true)) return null
    if (!checkCallableName(Boolean????::<!ELEMENT(2)!>, "<!ELEMENT_VALIDATION(2)!>") || Boolean????::<!ELEMENT(2)!>.get(false)) return null
    if (!checkCallableName(Boolean????::<!ELEMENT(2)!>::equals.call(false)::<!ELEMENT(1)!>, "<!ELEMENT_VALIDATION(1)!>")
        || !Boolean????::<!ELEMENT(2)!>::equals.call(false)::<!ELEMENT(1)!>.get()) return null
    if (!checkCallableName(Int?::<!ELEMENT(3)!>, "<!ELEMENT_VALIDATION(3)!>") || Int?::<!ELEMENT(3)!>.call(10, true)) return null

    return "OK"
}
