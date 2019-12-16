// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

interface Foo

fun test() {
    var nullable: Foo? = null
    val foo: Collection<Foo> = <!NI;TYPE_MISMATCH!>java.util.Collections.<!OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>singleton(nullable)<!><!>
    val foo1: Collection<Foo> = java.util.Collections.singleton(nullable!!)
}