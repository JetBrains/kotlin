<!DIRECTIVES("HELPERS: REFLECT")!>

package org.jetbrains.<!ELEMENT(1)!>

annotation class <!ELEMENT(2)!>

annotation class <!ELEMENT(3)!> <T> (val a: String)

class A {
    @field:<!ELEMENT(2)!>
    val x1: Int = 0

    @setparam:<!ELEMENT(2)!>
    var x2: Int = 1

    val @receiver:org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><List<Int>>("false") String.x3: Int
        get() {
            return 2
        }

    @org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><Int>("1")
    val x4: Int = 3

    @<!ELEMENT(2)!>
    val x5: Int = 4

    val x6: Int = "...".x3
}

@<!ELEMENT(2)!> val x6 = 5

@org.jetbrains.<!ELEMENT(1)!>.<!ELEMENT(3)!><<!ELEMENT(2)!>>(".") fun f1() = false

fun box(): String? {
    val a = A()

    if (a.x1 != 0 || a.x2 != 1 || a.x6 != 2 || a.x4 != 3 || a.x5 != 4 || x6 != 5) return null
    if (f1()) return null

    if (!checkProperties(A::class, listOf("x1", "x2", "x3", "x4", "x5"))) return null
    if (!checkPropertiesWithAnnotation(
            A::class,
            listOf(
                Pair("x4", listOf("org.jetbrains.<!ELEMENT_VALIDATION(1)!>.<!ELEMENT_VALIDATION(3)!>")),
                Pair("x5", listOf("org.jetbrains.<!ELEMENT_VALIDATION(1)!>.<!ELEMENT_VALIDATION(2)!>"))
            )
        )) return null
    if (!checkPropertyAnnotation(::x6, "org.jetbrains.<!ELEMENT_VALIDATION(1)!>.<!ELEMENT_VALIDATION(2)!>")) return null
    if (!checkFunctionAnnotation(::f1, "org.jetbrains.<!ELEMENT_VALIDATION(1)!>.<!ELEMENT_VALIDATION(3)!>")) return null

    return "OK"
}
