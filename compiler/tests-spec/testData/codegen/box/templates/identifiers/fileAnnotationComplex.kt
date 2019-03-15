<!DIRECTIVES("HELPERS: REFLECT")!>

@file:[org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(2)!>() <!ELEMENT(3)!>]

package org.jetbrains.<!ELEMENT(1)!>

@Target(AnnotationTarget.FILE)
annotation class <!ELEMENT(2)!>

@Target(AnnotationTarget.FILE)
annotation class <!ELEMENT(3)!>

fun box(): String? {
    if (!checkFileAnnotations("org.jetbrains.<!ELEMENT_VALIDATION(1)!>.<!CLASS_OF_FILE!>", listOf("org.jetbrains.<!ELEMENT_VALIDATION(1)!>.<!ELEMENT_VALIDATION(2)!>", "<!ELEMENT_VALIDATION(3)!>"))) return null
    if (!checkPackageName("org.jetbrains.<!ELEMENT_VALIDATION(1)!>.<!CLASS_OF_FILE!>", "org.jetbrains.<!ELEMENT_VALIDATION(1)!>")) return null
    if (!checkClassName(<!ELEMENT(2)!>::class, "org.jetbrains.<!ELEMENT_VALIDATION(1)!>.<!ELEMENT_VALIDATION(2)!>")) return null

    return "OK"
}
