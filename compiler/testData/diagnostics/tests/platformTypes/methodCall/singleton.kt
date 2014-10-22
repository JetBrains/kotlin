// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

trait Foo

fun test() {
    var nullable: Foo? = null
    val foo: Collection<Foo> = <!TYPE_MISMATCH!>java.util.Collections.singleton(nullable)<!>
    val foo1: Collection<Foo> = java.util.Collections.singleton(nullable!!)
}