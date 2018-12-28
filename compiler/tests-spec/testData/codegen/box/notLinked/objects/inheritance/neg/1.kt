/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 1
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25289
 * EXCEPTION: runtime
 */

open class Bar(val x: Int)

open class Foo {
    companion object : Bar(Foo.x)
}

fun box(): String? {
    if (Foo() == null) return null

    return "OK"
}
