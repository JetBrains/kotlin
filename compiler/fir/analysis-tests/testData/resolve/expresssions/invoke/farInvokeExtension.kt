// RUN_PIPELINE_TILL: FRONTEND


fun x() {}

operator fun Int.invoke(): Foo = this<!UNRESOLVED_LABEL!>@Foo<!>

class Foo {

    val x = 0

    fun foo() = x() // should resolve to fun x
}
