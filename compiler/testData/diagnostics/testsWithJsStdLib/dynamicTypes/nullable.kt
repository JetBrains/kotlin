// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(dn: dynamic<!REDUNDANT_NULLABLE!>?<!>, d: dynamic, dnn: dynamic<!REDUNDANT_NULLABLE!>?<!><!REDUNDANT_NULLABLE!>?<!>) {
    val a1 = dn.foo()
    a1.isDynamic()

    val a2 = dn?.foo()
    a2.isDynamic()

    val a3 = dn!!.foo()
    a3.isDynamic()

    d.foo()
    d?.foo()
    d!!.foo()
}
