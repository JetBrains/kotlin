/*
 * KOTLIN CODEGEN BOX NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 9
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-25289
 * EXCEPTION: runtime
 */

open class Foo(a: Any) {
     companion object : Foo(this)
}

fun box(): String? {
    if (Foo.Companion == null) return null

    return "OK"
}
