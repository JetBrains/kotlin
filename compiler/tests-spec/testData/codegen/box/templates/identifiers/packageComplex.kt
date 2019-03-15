<!DIRECTIVES("HELPERS: REFLECT")!>

package org.jetbrains.<!ELEMENT(1)!>

fun box(): String? {
    if (!checkPackageName("org.jetbrains.<!ELEMENT_VALIDATION(1)!>.<!CLASS_OF_FILE!>", "org.jetbrains.<!ELEMENT_VALIDATION(1)!>")) return null

    return "OK"
}
