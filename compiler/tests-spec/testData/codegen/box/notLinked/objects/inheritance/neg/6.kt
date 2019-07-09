/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 6
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25289
 * EXCEPTION: runtime
 */

open class Bar(val x: Any)

open class Foo {
    companion object : Bar(Foo.prop) {
         private val prop: Any = object {}
    }
}

fun box(): String? {
    if (Foo() == null) return null

    return "OK"
}