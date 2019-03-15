/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 14
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25289
 * EXCEPTION: runtime
 */

open class Foo(val prop: Int) {
    companion object MyCompanion : Foo(Foo.prop)
}

fun box(): String? {
    if (Foo(42) == null) return null

    return "OK"
}
