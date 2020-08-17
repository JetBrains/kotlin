

fun x() {}

operator fun Int.invoke(): Foo = <!UNRESOLVED_LABEL!>this@Foo<!>

class Foo {

    val x = 0

    fun foo() = x() // should resolve to fun x
}