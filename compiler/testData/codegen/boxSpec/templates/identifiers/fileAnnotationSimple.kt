<!DIRECTIVES("HELPERS: REFLECT")!>

@file:<!ELEMENT(1)!>
@file:<!ELEMENT(2)!>

@Target(AnnotationTarget.FILE)
annotation class <!ELEMENT(1)!>

@Target(AnnotationTarget.FILE)
annotation class <!ELEMENT(2)!>

fun box(): String? {
    if (!checkFileAnnotations("<!CLASS_OF_FILE!>", listOf("<!ELEMENT_VALIDATION(1)!>", "<!ELEMENT_VALIDATION(2)!>"))) return null
    if (!checkClassName(<!ELEMENT(1)!>::class, "<!ELEMENT_VALIDATION(1)!>")) return null
    if (!checkClassName(<!ELEMENT(2)!>::class, "<!ELEMENT_VALIDATION(2)!>")) return null

    return "OK"
}
