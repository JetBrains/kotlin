/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 1
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25289
 */

open class Bar(val x: Int)

open class Foo {
    companion object : Bar(Foo.prop) {
         const val prop: Int = 10
    }
}

fun box(): String? {
    Foo()

    return "OK"
}