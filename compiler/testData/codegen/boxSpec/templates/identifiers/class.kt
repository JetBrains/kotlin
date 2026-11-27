<!DIRECTIVES("HELPERS: REFLECT")!>

open class <!ELEMENT(1)!> {
    val x1 = true
}

internal open class A: <!ELEMENT(1)!>() {
    val x2 = false
}

annotation class <!ELEMENT(2)!>(val x2: Boolean)

@<!ELEMENT(2)!>(false) internal class B: @<!ELEMENT(2)!>(false) A() {}

@<!ELEMENT(2)!>(true) interface C

fun box(): String? {
    val o1 = <!ELEMENT(1)!>()
    val o2 = A()
    val o3 = B()

    if (o1.x1 != true) return null
    if (o2.x1 != true || o2.x2 != false || o3.x2 != false || o3.x1 != true) return null

    if (!checkAnnotation("B", "<!ELEMENT_VALIDATION(2)!>")) return null
    if (!checkAnnotation("C", "<!ELEMENT_VALIDATION(2)!>")) return null
    if (!checkSuperClass(B::class, "A")) return null
    if (!checkSuperTypeAnnotation(B::class, "A", "<!ELEMENT_VALIDATION(2)!>")) return null
    if (!checkClassName(<!ELEMENT(2)!>::class, "<!ELEMENT_VALIDATION(2)!>")) return null
    if (!checkClassName(<!ELEMENT(1)!>::class, "<!ELEMENT_VALIDATION(1)!>")) return null

    return "OK"
}