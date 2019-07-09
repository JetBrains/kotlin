/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 11
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25289
 * EXCEPTION: runtime
 */

object MyObject : Foo(prop)

open class Foo(val x: MyObject) {
    companion object {
        val prop = MyObject
    }
}

fun box(): String? {
    if (MyObject == null) return null

    return "OK"
}
