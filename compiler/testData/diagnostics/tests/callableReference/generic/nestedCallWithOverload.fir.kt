// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun foo(i: Int) {}
fun foo(s: String) {}
fun <T> id(x: T): T = x
fun <T> baz(x: T, y: T): T = TODO()

fun test() {
    val x1: (Int) -> Unit = id(id(::foo))
    val x2: (Int) -> Unit = baz(id(::foo), ::foo)
    val x3: (Int) -> Unit = baz(id(::foo), id(id(::foo)))
    val x4: (String) -> Unit = baz(id(::foo), id(id(::foo)))
    val x5: (Double) -> Unit = baz(id(<!UNRESOLVED_REFERENCE!>::foo<!>), id(id(<!UNRESOLVED_REFERENCE!>::foo<!>)))


    id<(Int) -> Unit>(id(id(<!UNRESOLVED_REFERENCE!>::foo<!>)))
    id(id<(Int) -> Unit>(::foo))
    baz<(Int) -> Unit>(id(<!UNRESOLVED_REFERENCE!>::foo<!>), id(id(<!UNRESOLVED_REFERENCE!>::foo<!>)))
    baz(id(<!UNRESOLVED_REFERENCE!>::foo<!>), id(id<(Int) -> Unit>(::foo)))
    baz(id(<!UNRESOLVED_REFERENCE!>::foo<!>), id<(Int) -> Unit>(id(::foo)))
}