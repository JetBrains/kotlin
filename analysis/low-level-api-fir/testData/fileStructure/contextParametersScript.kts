package one/* RootScriptStructureElement */

class A {/* ClassDeclarationStructureElement */

    context(p1: A, _: Short)
    fun foo(a: Int, b: String): Boolean = false/* DeclarationStructureElement */

    context(p1: A, _: Short)
    fun String.foo2(a: Int, b: String): Boolean = false/* DeclarationStructureElement */

    context(_: String, i: Int)
    val bar: Boolean/* DeclarationStructureElement */ get() = true

    context(_: String, i: Int)
    val Long.bar: Boolean/* DeclarationStructureElement */ get() = true
}

context(p1: A, _: Short)
fun foo(a: Int, b: String): Boolean = false/* DeclarationStructureElement */

context(p1: A, _: Short)
fun String.foo2(a: Int, b: String): Boolean = false/* DeclarationStructureElement */

context(_: String, i: Int)
val bar: Boolean/* DeclarationStructureElement */ get() = true

context(_: String, i: Int)
val Long.bar: Boolean/* DeclarationStructureElement */ get() = true


