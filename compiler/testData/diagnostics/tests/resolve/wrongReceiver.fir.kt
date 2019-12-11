// !WITH_NEW_INFERENCE
package some

class A()

val Int.some: Int get() = 4
val Int.foo: Int get() = 4

fun Int.extFun() = 4

fun String.test() {
    some
    some.A()
    "".<!INAPPLICABLE_CANDIDATE!>some<!>

    <!INAPPLICABLE_CANDIDATE!>foo<!>
    "".<!INAPPLICABLE_CANDIDATE!>foo<!>

    <!INAPPLICABLE_CANDIDATE!>extFun<!>()
    "".<!INAPPLICABLE_CANDIDATE!>extFun<!>()
}