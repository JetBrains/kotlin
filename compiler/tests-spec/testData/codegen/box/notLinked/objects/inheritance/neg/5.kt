/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 5
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25289
 * EXCEPTION: runtime
 */

open class Bar(val x: Int)

open class Foo {
    companion object : Bar(Foo.prop) {
        val prop: Int = 10
    }
}

fun box(): String? {
    if (Foo() == null) return null

    return "OK"
}