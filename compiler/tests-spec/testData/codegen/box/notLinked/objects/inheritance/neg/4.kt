/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 4
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25289
 * EXCEPTION: runtime
 */

open class Bar(val x: Int)

open class Foo {
     object MyObject : Bar(MyObject.x)
}

fun box(): String? {
    if (Foo.MyObject == null) return null

    return "OK"
}
