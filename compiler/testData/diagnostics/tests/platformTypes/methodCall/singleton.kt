// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

trait Foo

fun test() {
    var nullable: Foo? = null
    val foo: Collection<Foo> = java.util.Collections.singleton(nullable)
}